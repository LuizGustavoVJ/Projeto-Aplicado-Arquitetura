package com.pip.controller;

import com.pip.dto.AuthorizationRequest;
import com.pip.dto.CaptureRequest;
import com.pip.dto.VoidRequest;
import com.pip.dto.PaymentResponse;
import com.pip.model.Lojista;
import com.pip.model.Transacao;
import com.pip.model.TransactionStatus;
import com.pip.service.PagamentoService;
import com.pip.repository.LojistaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Controller responsável pelos endpoints da API de pagamentos
 * 
 * Implementa todos os endpoints do ciclo de vida de pagamentos:
 * - POST /authorize: Autorização de pagamento
 * - POST /{id}/capture: Captura de pagamento
 * - POST /{id}/void: Cancelamento de pagamento
 * - GET /{id}: Consulta de transação por ID
 * - GET /: Lista de transações com filtros
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagamentos", description = "Operações relacionadas ao ciclo de vida de um pagamento")
public class PagamentoController {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoController.class);

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private LojistaRepository lojistaRepository;

    /**
     * Autoriza um novo pagamento
     */
    @PostMapping("/authorize")
    @Operation(summary = "Autoriza um novo pagamento", 
               description = "Submete uma transação para autorização junto ao gateway de pagamento selecionado automaticamente")
    public ResponseEntity<PaymentResponse> autorizarPagamento(
            @Valid @RequestBody AuthorizationRequest request,
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Recebida requisição de autorização - Valor: {} {}", 
                   request.getAmount(), request.getCurrency());
        
        try {
            // Buscar lojista pela API Key
            Lojista lojista = lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Processar autorização
            PaymentResponse response = pagamentoService.autorizarPagamento(request, lojista);
            
            logger.info("Autorização processada - Transaction ID: {}, Status: {}", 
                       response.getTransactionId(), response.getStatus());
            
            return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro interno no processamento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        }
    }

    /**
     * Captura um pagamento autorizado
     */
    @PostMapping("/{transactionId}/capture")
    @Operation(summary = "Captura um pagamento autorizado", 
               description = "Efetiva a cobrança de um pagamento que foi previamente autorizado")
    public ResponseEntity<PaymentResponse> capturarPagamento(
            @PathVariable String transactionId, 
            @Valid @RequestBody CaptureRequest request,
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de captura recebida para transação: {}, Valor: {}", 
                   transactionId, request.getAmount());
        
        try {
            // Validar API Key
            lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Processar captura
            PaymentResponse response = pagamentoService.capturarPagamento(transactionId, request);
            
            logger.info("Captura processada com sucesso - Transaction ID: {}", transactionId);
            
            return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
                
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Erro ao capturar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("CAPTURE_ERROR", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro interno ao capturar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        }
    }

    /**
     * Cancela um pagamento autorizado
     */
    @PostMapping("/{transactionId}/void")
    @Operation(summary = "Cancela um pagamento autorizado", 
               description = "Cancela um pagamento que foi previamente autorizado ou capturado")
    public ResponseEntity<PaymentResponse> cancelarPagamento(
            @PathVariable String transactionId, 
            @Valid @RequestBody VoidRequest request,
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de cancelamento recebida para transação: {}", transactionId);
        
        try {
            // Validar API Key
            lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Processar cancelamento
            PaymentResponse response = pagamentoService.cancelarPagamento(transactionId, request);
            
            logger.info("Cancelamento processado com sucesso - Transaction ID: {}", transactionId);
            
            return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
                
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Erro ao cancelar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("VOID_ERROR", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro interno ao cancelar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        }
    }

    /**
     * Consulta uma transação pelo ID
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Consulta uma transação", 
               description = "Retorna os detalhes de uma transação específica")
    public ResponseEntity<PaymentResponse> consultarPagamento(
            @PathVariable String transactionId,
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de consulta recebida para transação: {}", transactionId);
        
        try {
            // Validar API Key
            lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Consultar transação
            PaymentResponse response = pagamentoService.consultarPagamento(transactionId);
            
            return ResponseEntity.ok(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Transação não encontrada: {}", transactionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse("NOT_FOUND", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro interno ao consultar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        }
    }

    /**
     * Lista transações com filtros
     */
    @GetMapping
    @Operation(summary = "Lista transações com filtros", 
               description = "Retorna uma lista paginada de transações com filtros opcionais")
    public ResponseEntity<Page<Transacao>> listarTransacoes(
            @Parameter(description = "Status da transação")
            @RequestParam(required = false) TransactionStatus status,
            
            @Parameter(description = "Data inicial (formato: ISO 8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime dataInicio,
            
            @Parameter(description = "Data final (formato: ISO 8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime dataFim,
            
            @Parameter(description = "Número da página (inicia em 0)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Campo para ordenação")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Direção da ordenação (ASC ou DESC)")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de listagem de transações - Page: {}, Size: {}", page, size);
        
        try {
            // Buscar lojista pela API Key
            Lojista lojista = lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Criar paginação
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            // Listar transações
            Page<Transacao> transacoes = pagamentoService.listarTransacoes(
                lojista.getId(), 
                status, 
                dataInicio, 
                dataFim, 
                pageable
            );
            
            logger.info("Listagem concluída - Total: {}, Página: {}/{}", 
                       transacoes.getTotalElements(), 
                       transacoes.getNumber() + 1, 
                       transacoes.getTotalPages());
            
            return ResponseEntity.ok(transacoes);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                
        } catch (Exception e) {
            logger.error("Erro interno ao listar transações", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cria resposta de erro padronizada
     */
    private PaymentResponse createErrorResponse(String errorCode, String errorMessage) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        response.setTimestamp(ZonedDateTime.now());
        return response;
    }
}
