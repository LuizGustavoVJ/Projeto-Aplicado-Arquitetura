package com.pip.service;

import com.pip.dto.AuthorizationRequest;
import com.pip.dto.CaptureRequest;
import com.pip.dto.VoidRequest;
import com.pip.dto.PaymentResponse;
import com.pip.model.*;
import com.pip.repository.TransacaoRepository;
import com.pip.repository.LogTransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável pela lógica de negócio de pagamentos
 * 
 * Implementa o fluxo completo de processamento de pagamentos:
 * - Autorização com seleção inteligente de gateway
 * - Captura com validações
 * - Cancelamento com auditoria
 * - Consulta com filtros
 * - Integração com webhooks
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class PagamentoService {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoService.class);

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private LogTransacaoRepository logTransacaoRepository;

    @Autowired
    private GatewayRoutingService gatewayRoutingService;

    @Autowired
    private GatewayIntegrationService gatewayIntegrationService;

    @Autowired
    private WebhookService webhookService;

    /**
     * Autoriza um novo pagamento
     * 
     * @param request Dados da requisição de autorização
     * @param lojista Lojista que está processando o pagamento
     * @return Resposta com os detalhes do pagamento processado
     */
    @Transactional
    public PaymentResponse autorizarPagamento(AuthorizationRequest request, Lojista lojista) {
        logger.info("Iniciando autorização de pagamento para lojista {}", lojista.getId());

        // Criar transação
        Transacao transacao = new Transacao();
        transacao.setTransactionId("TXN-" + UUID.randomUUID());
        transacao.setLojista(lojista);
        transacao.setValor(request.getAmount());
        transacao.setMoeda(request.getCurrency());
        transacao.setParcelas(request.getInstallments());
        transacao.setDescricao(request.getDescription());
        transacao.setStatus(TransactionStatus.PENDING.toString());
        transacao.setCreatedAt(ZonedDateTime.now());

        // Dados do cliente
        if (request.getCustomer() != null) {
            transacao.setCustomerName(request.getCustomer().get("name"));
            transacao.setCustomerEmail(request.getCustomer().get("email"));
            transacao.setCustomerDocument(request.getCustomer().get("document"));
        }

        // Salvar transação inicial
        transacao = transacaoRepository.save(transacao);

        // Registrar log
        registrarLog(transacao, "AUTHORIZATION_STARTED", "Iniciando processo de autorização");

        try {
            // Selecionar melhor gateway
            Gateway gateway = gatewayRoutingService.selecionarMelhorGateway(lojista, request.getAmount());
            transacao.setGateway(gateway);
            transacao = transacaoRepository.save(transacao);

            logger.info("Gateway selecionado: {} para transação {}", gateway.getCodigo(), transacao.getTransactionId());

            // Processar autorização no gateway
            PaymentResponse response = gatewayIntegrationService.authorize(gateway, request, transacao);

            // Atualizar transação com resposta
            if (response.isSuccess()) {
                transacao.setStatus(TransactionStatus.AUTHORIZED.toString());
                transacao.setGatewayTransactionId(response.getGatewayTransactionId());
                transacao.setAuthorizationCode(response.getAuthorizationCode());
                transacao.setNsu(response.getNsu());
                transacao.setTid(response.getTid());
                transacao.setAuthorizedAt(ZonedDateTime.now());

                registrarLog(transacao, "AUTHORIZATION_SUCCESS", "Autorização realizada com sucesso");

                // Criar webhook para notificar lojista
                webhookService.criarWebhook(lojista, transacao, "TRANSACTION_AUTHORIZED");

            } else {
                transacao.setStatus(TransactionStatus.FAILED.toString());
                transacao.setErrorCode(response.getErrorCode());
                transacao.setErrorMessage(response.getErrorMessage());

                registrarLog(transacao, "AUTHORIZATION_FAILED", "Falha na autorização: " + response.getErrorMessage());
            }

            transacao.setUpdatedAt(ZonedDateTime.now());
            transacao = transacaoRepository.save(transacao);

            return response;

        } catch (Exception e) {
            logger.error("Erro ao autorizar pagamento: {}", e.getMessage(), e);

            transacao.setStatus(TransactionStatus.FAILED.toString());
            transacao.setErrorMessage(e.getMessage());
            transacao.setUpdatedAt(ZonedDateTime.now());
            transacaoRepository.save(transacao);

            registrarLog(transacao, "AUTHORIZATION_ERROR", "Erro no processamento: " + e.getMessage());

            throw new RuntimeException("Falha ao processar autorização: " + e.getMessage(), e);
        }
    }

    /**
     * Captura um pagamento previamente autorizado
     * 
     * @param transactionId ID da transação a ser capturada
     * @param request Dados da captura
     * @return Resposta com os detalhes do pagamento capturado
     */
    @Transactional
    public PaymentResponse capturarPagamento(String transactionId, CaptureRequest request) {
        logger.info("Iniciando captura de pagamento {}", transactionId);

        // Buscar transação
        Transacao transacao = transacaoRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada: " + transactionId));

        // Validar status
        if (!transacao.getStatus().equals(TransactionStatus.AUTHORIZED.toString())) {
            throw new IllegalStateException("Transação não pode ser capturada. Status atual: " + transacao.getStatus());
        }

        // Validar valor
        if (request.getAmount().compareTo(BigDecimal.valueOf(transacao.getValor())) > 0) {
            throw new IllegalArgumentException("Valor da captura não pode ser maior que o valor autorizado");
        }

        registrarLog(transacao, "CAPTURE_STARTED", "Iniciando processo de captura");

        try {
            // Processar captura no gateway
            PaymentResponse response = gatewayIntegrationService.capture(transacao.getGateway(), request, transacao);

            // Atualizar transação
            if (response.isSuccess()) {
                transacao.setStatus(TransactionStatus.CAPTURED.toString());
                transacao.setValorCapturado(request.getAmount().longValue());
                transacao.setCapturedAt(ZonedDateTime.now());

                registrarLog(transacao, "CAPTURE_SUCCESS", "Captura realizada com sucesso");

                // Criar webhook
                webhookService.criarWebhook(transacao.getLojista(), transacao, "TRANSACTION_CAPTURED");

            } else {
                transacao.setStatus(TransactionStatus.FAILED.toString());
                transacao.setErrorCode(response.getErrorCode());
                transacao.setErrorMessage(response.getErrorMessage());

                registrarLog(transacao, "CAPTURE_FAILED", "Falha na captura: " + response.getErrorMessage());
            }

            transacao.setUpdatedAt(ZonedDateTime.now());
            transacaoRepository.save(transacao);

            return response;

        } catch (Exception e) {
            logger.error("Erro ao capturar pagamento: {}", e.getMessage(), e);

            transacao.setStatus(TransactionStatus.FAILED.toString());
            transacao.setErrorMessage(e.getMessage());
            transacao.setUpdatedAt(ZonedDateTime.now());
            transacaoRepository.save(transacao);

            registrarLog(transacao, "CAPTURE_ERROR", "Erro no processamento: " + e.getMessage());

            throw new RuntimeException("Falha ao processar captura: " + e.getMessage(), e);
        }
    }

    /**
     * Cancela um pagamento autorizado
     * 
     * @param transactionId ID da transação a ser cancelada
     * @param request Dados do cancelamento
     * @return Resposta com os detalhes do pagamento cancelado
     */
    @Transactional
    public PaymentResponse cancelarPagamento(String transactionId, VoidRequest request) {
        logger.info("Iniciando cancelamento de pagamento {}", transactionId);

        // Buscar transação
        Transacao transacao = transacaoRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada: " + transactionId));

        // Validar status
        if (!transacao.getStatus().equals(TransactionStatus.AUTHORIZED.toString()) && 
            !transacao.getStatus().equals(TransactionStatus.CAPTURED.toString())) {
            throw new IllegalStateException("Transação não pode ser cancelada. Status atual: " + transacao.getStatus());
        }

        registrarLog(transacao, "VOID_STARTED", "Iniciando processo de cancelamento");

        try {
            // Processar cancelamento no gateway
            PaymentResponse response = gatewayIntegrationService.voidTransaction(transacao.getGateway(), request, transacao);

            // Atualizar transação
            if (response.isSuccess()) {
                transacao.setStatus(TransactionStatus.VOIDED.toString());
                transacao.setVoidReason(request.getReason());
                transacao.setVoidedAt(ZonedDateTime.now());

                registrarLog(transacao, "VOID_SUCCESS", "Cancelamento realizado com sucesso");

                // Criar webhook
                webhookService.criarWebhook(transacao.getLojista(), transacao, "TRANSACTION_VOIDED");

            } else {
                transacao.setErrorCode(response.getErrorCode());
                transacao.setErrorMessage(response.getErrorMessage());

                registrarLog(transacao, "VOID_FAILED", "Falha no cancelamento: " + response.getErrorMessage());
            }

            transacao.setUpdatedAt(ZonedDateTime.now());
            transacaoRepository.save(transacao);

            return response;

        } catch (Exception e) {
            logger.error("Erro ao cancelar pagamento: {}", e.getMessage(), e);

            transacao.setErrorMessage(e.getMessage());
            transacao.setUpdatedAt(ZonedDateTime.now());
            transacaoRepository.save(transacao);

            registrarLog(transacao, "VOID_ERROR", "Erro no processamento: " + e.getMessage());

            throw new RuntimeException("Falha ao processar cancelamento: " + e.getMessage(), e);
        }
    }

    /**
     * Consulta uma transação pelo ID
     * 
     * @param transactionId ID da transação
     * @return Resposta com os detalhes da transação
     */
    public PaymentResponse consultarPagamento(String transactionId) {
        logger.info("Consultando pagamento {}", transactionId);

        Transacao transacao = transacaoRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada: " + transactionId));

        PaymentResponse response = new PaymentResponse();
        response.setSuccess(transacao.getStatus().equals(TransactionStatus.AUTHORIZED || 
                           transacao.getStatus().equals(TransactionStatus.CAPTURED);
        response.setStatus(transacao.getStatus().name());
        response.setTransactionId(transacao.getTransactionId());
        response.setGatewayTransactionId(transacao.getGatewayTransactionId());
        response.setAuthorizationCode(transacao.getAuthorizationCode());
        response.setNsu(transacao.getNsu());
        response.setTid(transacao.getTid());
        response.setErrorCode(transacao.getErrorCode());
        response.setErrorMessage(transacao.getErrorMessage());
        response.setTimestamp(transacao.getCreatedAt());

        return response;
    }

    /**
     * Lista transações com filtros
     * 
     * @param lojistaId ID do lojista (opcional)
     * @param status Status da transação (opcional)
     * @param dataInicio Data inicial (opcional)
     * @param dataFim Data final (opcional)
     * @param pageable Paginação
     * @return Página de transações
     */
    public Page<Transacao> listarTransacoes(UUID lojistaId, TransactionStatus status, 
                                            ZonedDateTime dataInicio, ZonedDateTime dataFim, 
                                            Pageable pageable) {
        logger.info("Listando transações com filtros");

        Specification<Transacao> spec = Specification.where(null);

        if (lojistaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("lojista").get("id"), lojistaId));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (dataInicio != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dataInicio));
        }

        if (dataFim != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), dataFim));
        }

        return transacaoRepository.findAll(spec, pageable);
    }

    /**
     * Registra log de transação
     */
    private void registrarLog(Transacao transacao, String evento, String descricao) {
        LogTransacao log = new LogTransacao();
        log.setTransacao(transacao);
        log.setEvento(evento);
        log.setDescricao(descricao);
        log.setTimestamp(ZonedDateTime.now());
        logTransacaoRepository.save(log);
    }
}
