package com.pip.controller;

import com.pip.dto.AuthorizationRequest;
import com.pip.dto.PaymentResponse;
import com.pip.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller responsável pelos endpoints da API de pagamentos
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Pagamentos", description = "Operações relacionadas ao ciclo de vida de um pagamento")
public class PagamentoController {

    @Autowired
    private PagamentoService pagamentoService;

    @PostMapping("/authorize")
    @Operation(summary = "Autoriza um novo pagamento", 
               description = "Submete uma transação para autorização junto ao gateway de pagamento")
    public ResponseEntity<PaymentResponse> autorizarPagamento(@Valid @RequestBody AuthorizationRequest request) {
        PaymentResponse response = pagamentoService.autorizarPagamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/capture")
    @Operation(summary = "Captura um pagamento autorizado", 
               description = "Efetiva a cobrança de um pagamento que foi previamente autorizado")
    public ResponseEntity<PaymentResponse> capturarPagamento(@PathVariable UUID id) {
        PaymentResponse response = pagamentoService.capturarPagamento(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "Cancela um pagamento autorizado", 
               description = "Anula uma autorização de pagamento que ainda não foi capturada")
    public ResponseEntity<PaymentResponse> cancelarPagamento(@PathVariable UUID id) {
        PaymentResponse response = pagamentoService.cancelarPagamento(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um pagamento", 
               description = "Recupera os detalhes e o status atual de um pagamento específico")
    public ResponseEntity<PaymentResponse> consultarPagamento(@PathVariable UUID id) {
        PaymentResponse response = pagamentoService.consultarPagamento(id);
        return ResponseEntity.ok(response);
    }
}

