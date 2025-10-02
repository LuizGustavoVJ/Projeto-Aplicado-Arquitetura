package com.pip.service;

import com.pip.dto.AuthorizationRequest;
import com.pip.model.Gateway;
import com.pip.model.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de Marketplace do Mercado Pago
 * 
 * Permite criar e gerenciar marketplaces com múltiplos vendedores
 * 
 * Funcionalidades:
 * - Criação de sub-contas de vendedores
 * - Split automático de pagamentos
 * - Gestão de comissões
 * - Repasse automático
 * - Relatórios por vendedor
 * 
 * Documentação: https://www.mercadopago.com.br/developers/pt/docs/mp-point/integration-configuration/integrate-with-marketplace
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class MarketplaceService {

    private static final Logger logger = LoggerFactory.getLogger(MarketplaceService.class);

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Cria pagamento com split para marketplace
     * 
     * @param gateway Gateway Mercado Pago
     * @param request Requisição de autorização
     * @param transacao Transação
     * @param sellerId ID do vendedor
     * @param marketplaceFee Taxa do marketplace (percentual)
     * @return Resultado do processamento
     */
    public Map<String, Object> createMarketplacePayment(
            Gateway gateway,
            AuthorizationRequest request,
            Transacao transacao,
            String sellerId,
            Double marketplaceFee) {

        logger.info("[MARKETPLACE] Criando pagamento - Seller: {} - Fee: {}%", 
            sellerId, marketplaceFee);

        try {
            // Validar parâmetros
            if (sellerId == null || sellerId.isEmpty()) {
                throw new IllegalArgumentException("Seller ID obrigatório");
            }

            if (marketplaceFee == null || marketplaceFee < 0 || marketplaceFee > 100) {
                throw new IllegalArgumentException("Taxa do marketplace inválida");
            }

            // Construir payload
            Map<String, Object> payload = buildMarketplacePayload(
                request, transacao, sellerId, marketplaceFee
            );

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());
            headers.set("X-Idempotency-Key", transacao.getTransactionId());

            // Fazer requisição
            String url = gateway.getApiUrl() + "/v1/payments";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                logger.info("[MARKETPLACE] Pagamento criado com sucesso");
                return response.getBody();
            } else {
                throw new RuntimeException("Falha ao criar pagamento marketplace");
            }

        } catch (Exception e) {
            logger.error("[MARKETPLACE] Erro ao criar pagamento", e);
            throw new RuntimeException("Erro no marketplace", e);
        }
    }

    /**
     * Constrói payload para pagamento marketplace
     */
    private Map<String, Object> buildMarketplacePayload(
            AuthorizationRequest request,
            Transacao transacao,
            String sellerId,
            Double marketplaceFee) {

        Map<String, Object> payload = new HashMap<>();

        // Dados básicos
        payload.put("transaction_amount", request.getAmount());
        payload.put("token", request.getCardToken());
        payload.put("installments", request.getInstallments() != null ? request.getInstallments() : 1);
        payload.put("payment_method_id", "credit_card");
        payload.put("description", "Pagamento via PIP Marketplace");

        // Dados do pagador
        if (request.getCustomer() != null) {
            Map<String, Object> payer = new HashMap<>();
            payer.put("email", request.getCustomer().get("email"));
            payer.put("identification", Map.of(
                "type", "CPF",
                "number", request.getCustomer().get("document")
            ));
            payload.put("payer", payer);
        }

        // Configuração de marketplace
        payload.put("marketplace", "PIP");
        payload.put("marketplace_fee", calculateMarketplaceFee(request.getAmount(), marketplaceFee));

        // Dados do vendedor (collector)
        payload.put("collector_id", sellerId);

        // Metadados
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pip_transaction_id", transacao.getTransactionId());
        metadata.put("seller_id", sellerId);
        metadata.put("marketplace_fee_percent", marketplaceFee);
        payload.put("metadata", metadata);

        return payload;
    }

    /**
     * Calcula taxa do marketplace
     */
    private double calculateMarketplaceFee(Double amount, Double feePercent) {
        return (amount * feePercent) / 100.0;
    }

    /**
     * Cria sub-conta de vendedor no marketplace
     * 
     * @param gateway Gateway Mercado Pago
     * @param sellerData Dados do vendedor
     * @return ID do vendedor criado
     */
    public String createSeller(Gateway gateway, Map<String, Object> sellerData) {
        logger.info("[MARKETPLACE] Criando vendedor: {}", sellerData.get("email"));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/v1/marketplace/sellers";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(sellerData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> responseBody = response.getBody();
                String sellerId = (String) responseBody.get("id");
                logger.info("[MARKETPLACE] Vendedor criado: {}", sellerId);
                return sellerId;
            } else {
                throw new RuntimeException("Falha ao criar vendedor");
            }

        } catch (Exception e) {
            logger.error("[MARKETPLACE] Erro ao criar vendedor", e);
            throw new RuntimeException("Erro ao criar vendedor", e);
        }
    }

    /**
     * Lista transações de um vendedor
     * 
     * @param gateway Gateway Mercado Pago
     * @param sellerId ID do vendedor
     * @return Lista de transações
     */
    public List<Map<String, Object>> getSellerTransactions(Gateway gateway, String sellerId) {
        logger.info("[MARKETPLACE] Listando transações do vendedor: {}", sellerId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/v1/payments/search?collector_id=" + sellerId;
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                return (List<Map<String, Object>>) responseBody.get("results");
            } else {
                throw new RuntimeException("Falha ao listar transações");
            }

        } catch (Exception e) {
            logger.error("[MARKETPLACE] Erro ao listar transações", e);
            return new ArrayList<>();
        }
    }
}
