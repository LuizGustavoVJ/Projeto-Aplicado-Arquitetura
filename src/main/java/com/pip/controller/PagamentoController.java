package com.pip.controller;

import com.pip.dto.AuthorizationRequest;
import com.pip.dto.CaptureRequest;
import com.pip.dto.VoidRequest;
import com.pip.dto.PaymentResponse;
import com.pip.dto.GatewayResponse;
import com.pip.service.PagamentoService;
import com.pip.service.GatewayIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller responsável pelos endpoints da API de pagamentos - PoC Implementada
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Pagamentos", description = "Operações relacionadas ao ciclo de vida de um pagamento")
public class PagamentoController {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoController.class);

    @Autowired
    private PagamentoService pagamentoService;
    
    @Autowired
    private GatewayIntegrationService gatewayIntegrationService;

    /**
     * Endpoint para autorização de pagamento - PoC Funcional
     */
    @PostMapping("/authorize")
    @Operation(summary = "Autoriza um novo pagamento", 
               description = "Submete uma transação para autorização junto ao gateway de pagamento")
    public ResponseEntity<PaymentResponse> autorizarPagamento(@Valid @RequestBody AuthorizationRequest request) {
        logger.info("Recebida requisição de autorização - Valor: {} {}", 
                   request.getAmount(), request.getCurrency());
        
        try {
            // Validações de entrada
            validateAuthorizationRequest(request);
            
            // Processa transação via gateway integration service
            GatewayResponse gatewayResponse = gatewayIntegrationService.processTransaction(
                request.getCardToken(), 
                request.getAmount(), 
                request.getCurrency()
            );
            
            // Converte resposta do gateway para resposta da API
            PaymentResponse response = mapToPaymentResponse(gatewayResponse, request);
            
            // Determina status HTTP baseado no resultado
            HttpStatus httpStatus = determineHttpStatus(gatewayResponse.getStatus());
            
            logger.info("Autorização processada - ID: {}, Status: {}", 
                       response.getId(), response.getStatus());
            
            return ResponseEntity.status(httpStatus).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro interno no processamento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        }
    }

    @PostMapping("/{id}/capture")
    @Operation(summary = "Captura um pagamento autorizado", 
               description = "Efetiva a cobrança de um pagamento que foi previamente autorizado")
    public ResponseEntity<PaymentResponse> capturarPagamento(
            @PathVariable UUID id, 
            @Valid @RequestBody CaptureRequest request) {
        
        logger.info("Requisição de captura recebida para ID: {}, Valor: {}", id, request.getAmount());
        
        try {
            PaymentResponse response = pagamentoService.capturarPagamento(id, request);
            logger.info("Captura processada com sucesso - ID: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação na captura: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
                
        } catch (IllegalStateException e) {
            logger.warn("Estado inválido para captura: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse("INVALID_STATE", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro interno na captura", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        }
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "Cancela um pagamento autorizado", 
               description = "Anula uma autorização de pagamento que ainda não foi capturada")
    public ResponseEntity<PaymentResponse> cancelarPagamento(
            @PathVariable UUID id, 
            @Valid @RequestBody VoidRequest request) {
        
        logger.info("Requisição de cancelamento recebida para ID: {}, Motivo: {}", id, request.getReason());
        
        try {
            PaymentResponse response = pagamentoService.cancelarPagamento(id, request);
            logger.info("Cancelamento processado com sucesso - ID: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação no cancelamento: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
                
        } catch (IllegalStateException e) {
            logger.warn("Estado inválido para cancelamento: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse("INVALID_STATE", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro interno no cancelamento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um pagamento", 
               description = "Recupera os detalhes e o status atual de um pagamento específico")
    public ResponseEntity<PaymentResponse> consultarPagamento(@PathVariable UUID id) {
        logger.info("Consulta de pagamento recebida para ID: {}", id);
        
        try {
            PaymentResponse response = pagamentoService.consultarPagamento(id);
            logger.info("Consulta processada com sucesso - ID: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Pagamento não encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Erro interno na consulta", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        }
    }

    @GetMapping
    @Operation(summary = "Lista pagamentos com filtros", 
               description = "Recupera uma lista paginada de pagamentos com filtros opcionais")
    public ResponseEntity<Page<PaymentResponse>> listarPagamentos(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Listagem de pagamentos - Status: {}, Gateway: {}, Página: {}", status, gateway, page);
        
        try {
            // TODO: Implementar filtros e paginação na próxima iteração
            // Por enquanto, retorna uma página vazia
            Pageable pageable = PageRequest.of(page, size);
            Page<PaymentResponse> emptyPage = Page.empty(pageable);
            
            return ResponseEntity.ok(emptyPage);
            
        } catch (Exception e) {
            logger.error("Erro interno na listagem", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Valida dados da requisição de autorização
     */
    private void validateAuthorizationRequest(AuthorizationRequest request) {
        if (request.getCardToken() == null || request.getCardToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Token do cartão é obrigatório");
        }
        
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
        
        if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Moeda é obrigatória");
        }
    }
    
    /**
     * Mapeia resposta do gateway para resposta da API
     */
    private PaymentResponse mapToPaymentResponse(GatewayResponse gatewayResponse, AuthorizationRequest request) {
        PaymentResponse response = new PaymentResponse();
        response.setId(UUID.randomUUID().toString());
        response.setAmount(request.getAmount());
        response.setCurrency(request.getCurrency());
        response.setCreatedAt(LocalDateTime.now());
        
        // Mapeia status do gateway para status da API
        switch (gatewayResponse.getStatus()) {
            case "APPROVED":
                response.setStatus("authorized");
                response.setMessage("Pagamento autorizado com sucesso");
                response.setAuthorizationCode(gatewayResponse.getAuthorizationCode());
                break;
            case "DECLINED":
                response.setStatus("declined");
                response.setMessage(gatewayResponse.getMessage());
                response.setErrorCode(gatewayResponse.getErrorCode());
                break;
            case "ERROR":
                response.setStatus("failed");
                response.setMessage(gatewayResponse.getMessage());
                response.setErrorCode(gatewayResponse.getErrorCode());
                break;
            default:
                response.setStatus("unknown");
                response.setMessage("Status desconhecido do gateway");
        }
        
        return response;
    }
    
    /**
     * Determina status HTTP baseado no status da transação
     */
    private HttpStatus determineHttpStatus(String gatewayStatus) {
        switch (gatewayStatus) {
            case "APPROVED":
                return HttpStatus.OK;
            case "DECLINED":
                return HttpStatus.PAYMENT_REQUIRED;
            case "ERROR":
                return HttpStatus.BAD_GATEWAY;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
    
    /**
     * Cria resposta de erro padronizada
     */
    private PaymentResponse createErrorResponse(String errorCode, String message) {
        PaymentResponse response = new PaymentResponse();
        response.setId(UUID.randomUUID().toString());
        response.setStatus("failed");
        response.setErrorCode(errorCode);
        response.setMessage(message);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}

