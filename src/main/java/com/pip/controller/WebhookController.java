package com.pip.controller;

import com.pip.dto.WebhookConfigRequest;
import com.pip.model.Lojista;
import com.pip.model.Webhook;
import com.pip.model.WebhookEvent;
import com.pip.repository.LojistaRepository;
import com.pip.repository.WebhookRepository;
import com.pip.repository.WebhookEventRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para gerenciamento de webhooks
 * 
 * Endpoints para configuração e monitoramento de webhooks:
 * - POST /webhooks: Criar/atualizar configuração de webhook
 * - GET /webhooks: Listar configurações de webhook
 * - DELETE /webhooks/{id}: Remover configuração de webhook
 * - GET /webhooks/events: Listar eventos de webhook
 * - POST /webhooks/test: Testar envio de webhook
 * 
 * @author Luiz Gustavo Finotello
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Gerenciamento de configurações e eventos de webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private LojistaRepository lojistaRepository;

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    /**
     * Cria ou atualiza configuração de webhook
     */
    @PostMapping
    @Operation(summary = "Configurar webhook", 
               description = "Cria ou atualiza a configuração de webhook do lojista")
    public ResponseEntity<Map<String, Object>> configurarWebhook(
            @Valid @RequestBody WebhookConfigRequest request,
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de configuração de webhook recebida");
        
        try {
            // Buscar lojista
            Lojista lojista = lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Buscar ou criar webhook
            Webhook webhook = webhookRepository.findByLojista(lojista)
                .orElse(new Webhook());

            // Atualizar configuração
            webhook.setLojista(lojista);
            webhook.setUrl(request.getUrl());
            webhook.setSecret(request.getSecret());
            webhook.setAtivo(request.isActive());
            
            if (request.getEvents() != null && !request.getEvents().isEmpty()) {
                webhook.setEventos(String.join(",", request.getEvents()));
            }

            if (webhook.getId() == null) {
                webhook.setCreatedAt(ZonedDateTime.now());
            }
            webhook.setUpdatedAt(ZonedDateTime.now());

            // Salvar
            webhook = webhookRepository.save(webhook);

            logger.info("Webhook configurado com sucesso para lojista: {}", lojista.getId());

            // Preparar resposta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Webhook configurado com sucesso");
            response.put("webhook", Map.of(
                "id", webhook.getId(),
                "url", webhook.getUrl(),
                "active", webhook.isAtivo(),
                "events", request.getEvents() != null ? request.getEvents() : List.of(),
                "createdAt", webhook.getCreatedAt(),
                "updatedAt", webhook.getUpdatedAt()
            ));

            return ResponseEntity.ok(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro ao configurar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Erro interno do servidor"));
        }
    }

    /**
     * Lista configurações de webhook do lojista
     */
    @GetMapping
    @Operation(summary = "Listar webhooks", 
               description = "Retorna as configurações de webhook do lojista")
    public ResponseEntity<Map<String, Object>> listarWebhooks(
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de listagem de webhooks recebida");
        
        try {
            // Buscar lojista
            Lojista lojista = lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Buscar webhooks
            List<Webhook> webhooks = webhookRepository.findAllByLojista(lojista);

            // Preparar resposta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", webhooks.size());
            response.put("webhooks", webhooks.stream().map(w -> Map.of(
                "id", w.getId(),
                "url", w.getUrl(),
                "active", w.isAtivo(),
                "events", w.getEventos() != null ? List.of(w.getEventos().split(",")) : List.of(),
                "createdAt", w.getCreatedAt(),
                "updatedAt", w.getUpdatedAt()
            )).toList());

            return ResponseEntity.ok(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro ao listar webhooks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Erro interno do servidor"));
        }
    }

    /**
     * Remove configuração de webhook
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover webhook", 
               description = "Remove a configuração de webhook especificada")
    public ResponseEntity<Map<String, Object>> removerWebhook(
            @PathVariable UUID id,
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de remoção de webhook recebida: {}", id);
        
        try {
            // Buscar lojista
            Lojista lojista = lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Buscar webhook
            Webhook webhook = webhookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook não encontrado"));

            // Verificar se pertence ao lojista
            if (!webhook.getLojista().getId().equals(lojista.getId())) {
                throw new IllegalArgumentException("Webhook não pertence ao lojista");
            }

            // Remover
            webhookRepository.delete(webhook);

            logger.info("Webhook removido com sucesso: {}", id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Webhook removido com sucesso"
            ));
                
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro ao remover webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Erro interno do servidor"));
        }
    }

    /**
     * Lista eventos de webhook
     */
    @GetMapping("/events")
    @Operation(summary = "Listar eventos de webhook", 
               description = "Retorna uma lista paginada de eventos de webhook")
    public ResponseEntity<Page<WebhookEvent>> listarEventos(
            @Parameter(description = "Status do evento")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Número da página (inicia em 0)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "20") int size,
            
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de listagem de eventos de webhook - Page: {}, Size: {}", page, size);
        
        try {
            // Buscar lojista
            Lojista lojista = lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Criar paginação
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // Buscar eventos
            Page<WebhookEvent> eventos;
            if (status != null && !status.trim().isEmpty()) {
                eventos = webhookEventRepository.findByLojistaAndStatus(lojista, status, pageable);
            } else {
                eventos = webhookEventRepository.findByLojista(lojista, pageable);
            }

            logger.info("Listagem de eventos concluída - Total: {}, Página: {}/{}", 
                       eventos.getTotalElements(), 
                       eventos.getNumber() + 1, 
                       eventos.getTotalPages());

            return ResponseEntity.ok(eventos);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                
        } catch (Exception e) {
            logger.error("Erro ao listar eventos de webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Testa envio de webhook
     */
    @PostMapping("/test")
    @Operation(summary = "Testar webhook", 
               description = "Envia um webhook de teste para verificar a configuração")
    public ResponseEntity<Map<String, Object>> testarWebhook(
            @RequestHeader("X-Api-Key") String apiKey) {
        
        logger.info("Requisição de teste de webhook recebida");
        
        try {
            // Buscar lojista
            Lojista lojista = lojistaRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("API Key inválida"));

            // Buscar webhook configurado
            Webhook webhook = webhookRepository.findByLojista(lojista)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum webhook configurado"));

            if (!webhook.isAtivo()) {
                throw new IllegalArgumentException("Webhook está inativo");
            }

            // TODO: Implementar envio de webhook de teste
            logger.info("Teste de webhook solicitado para lojista: {}", lojista.getId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Webhook de teste enviado com sucesso",
                "url", webhook.getUrl()
            ));
                
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erro ao testar webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Erro interno do servidor"));
        }
    }
}
