package com.pip.service;

import com.pip.dto.AuthorizationRequest;
import com.pip.dto.CaptureRequest;
import com.pip.dto.VoidRequest;
import com.pip.dto.PaymentResponse;
import com.pip.model.Transacao;
import com.pip.model.TransactionStatus;
import com.pip.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/**
 * Serviço responsável pela lógica de negócio de pagamentos
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class PagamentoService {

    @Autowired
    private TransacaoRepository transacaoRepository;

    /**
     * Autoriza um novo pagamento
     * 
     * @param request Dados da requisição de autorização
     * @return Resposta com os detalhes do pagamento processado
     */
    public PaymentResponse autorizarPagamento(AuthorizationRequest request) {
        // TODO: Implementar lógica de autorização na Sprint 03
        
        // Por enquanto, simula uma autorização bem-sucedida
        Transacao transacao = new Transacao(
            UUID.randomUUID(), // lojistaId simulado
            request.getAmount(),
            "AUTHORIZED",
            request.getCardToken()
        );
        
        transacao = transacaoRepository.save(transacao);
        
        Map<String, Object> gatewayDetails = new HashMap<>();
        gatewayDetails.put("authorizationCode", "AUTH123456");
        gatewayDetails.put("gatewayTransactionId", "GW789012");
        
        return new PaymentResponse(
            transacao.getId(),
            transacao.getStatus(),
            transacao.getValor(),
            gatewayDetails
        );
    }

    /**
     * Captura um pagamento previamente autorizado
     * 
     * @param paymentId ID do pagamento a ser capturado
     * @param request Dados da captura (valor e descrição)
     * @return Resposta com os detalhes do pagamento capturado
     */
    @Transactional
    public PaymentResponse capturarPagamento(UUID paymentId, CaptureRequest request) {
        Optional<Transacao> transacaoOpt = transacaoRepository.findById(paymentId);
        
        if (transacaoOpt.isEmpty()) {
            throw new IllegalArgumentException("Pagamento não encontrado: " + paymentId);
        }
        
        Transacao transacao = transacaoOpt.get();
        TransactionStatus currentStatus = TransactionStatus.fromCode(transacao.getStatus());
        
        if (!currentStatus.canCapture()) {
            throw new IllegalStateException("Pagamento não pode ser capturado. Status atual: " + currentStatus);
        }
        
        // Validar valor da captura
        BigDecimal valorOriginal = BigDecimal.valueOf(transacao.getValor()).divide(BigDecimal.valueOf(100));
        if (request.getAmount().compareTo(valorOriginal) > 0) {
            throw new IllegalArgumentException("Valor da captura não pode ser maior que o valor autorizado");
        }
        
        // Atualizar status da transação
        transacao.setStatus(TransactionStatus.CAPTURED.getCode());
        transacao.setUpdatedAt(ZonedDateTime.now());
        
        // Simular captura no gateway (será implementado com gateways reais na Fase 2)
        transacao = transacaoRepository.save(transacao);
        
        Map<String, Object> gatewayDetails = new HashMap<>();
        gatewayDetails.put("captureCode", "CAP" + System.currentTimeMillis());
        gatewayDetails.put("capturedAmount", request.getAmount());
        gatewayDetails.put("description", request.getDescription());
        gatewayDetails.put("capturedAt", ZonedDateTime.now());
        
        return new PaymentResponse(
            transacao.getId(),
            transacao.getStatus(),
            transacao.getValor(),
            gatewayDetails
        );
    }

    /**
     * Cancela um pagamento autorizado
     * 
     * @param paymentId ID do pagamento a ser cancelado
     * @param request Dados do cancelamento (motivo e observações)
     * @return Resposta com os detalhes do pagamento cancelado
     */
    @Transactional
    public PaymentResponse cancelarPagamento(UUID paymentId, VoidRequest request) {
        Optional<Transacao> transacaoOpt = transacaoRepository.findById(paymentId);
        
        if (transacaoOpt.isEmpty()) {
            throw new IllegalArgumentException("Pagamento não encontrado: " + paymentId);
        }
        
        Transacao transacao = transacaoOpt.get();
        TransactionStatus currentStatus = TransactionStatus.fromCode(transacao.getStatus());
        
        if (!currentStatus.canVoid()) {
            throw new IllegalStateException("Pagamento não pode ser cancelado. Status atual: " + currentStatus);
        }
        
        // Atualizar status da transação
        transacao.setStatus(TransactionStatus.VOIDED.getCode());
        transacao.setUpdatedAt(ZonedDateTime.now());
        
        // Simular cancelamento no gateway (será implementado com gateways reais na Fase 2)
        transacao = transacaoRepository.save(transacao);
        
        Map<String, Object> gatewayDetails = new HashMap<>();
        gatewayDetails.put("voidCode", "VOID" + System.currentTimeMillis());
        gatewayDetails.put("reason", request.getReason());
        gatewayDetails.put("notes", request.getNotes());
        gatewayDetails.put("voidedAt", ZonedDateTime.now());
        
        return new PaymentResponse(
            transacao.getId(),
            transacao.getStatus(),
            transacao.getValor(),
            gatewayDetails
        );
    }

    /**
     * Consulta um pagamento pelo ID
     * 
     * @param paymentId ID do pagamento a ser consultado
     * @return Resposta com os detalhes do pagamento
     */
    public PaymentResponse consultarPagamento(UUID paymentId) {
        Optional<Transacao> transacaoOpt = transacaoRepository.findById(paymentId);
        
        if (transacaoOpt.isEmpty()) {
            throw new IllegalArgumentException("Pagamento não encontrado: " + paymentId);
        }
        
        Transacao transacao = transacaoOpt.get();
        
        Map<String, Object> gatewayDetails = new HashMap<>();
        gatewayDetails.put("transactionId", transacao.getId());
        gatewayDetails.put("lojistaId", transacao.getLojistaId());
        gatewayDetails.put("cardToken", transacao.getCardToken());
        gatewayDetails.put("gatewayId", transacao.getGatewayId());
        gatewayDetails.put("createdAt", transacao.getCreatedAt());
        gatewayDetails.put("updatedAt", transacao.getUpdatedAt());
        
        return new PaymentResponse(
            transacao.getId(),
            transacao.getStatus(),
            transacao.getValor(),
            gatewayDetails
        );
    }
}

