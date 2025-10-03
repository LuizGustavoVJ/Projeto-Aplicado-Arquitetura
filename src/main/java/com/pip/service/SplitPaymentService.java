package com.pip.service;

import com.pip.dto.SplitRequest;
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
 * Serviço para gerenciar Split de Pagamentos
 * 
 * Permite dividir o valor de uma transação entre múltiplos recebedores
 * Suportado por: PagSeguro, Mercado Pago
 * 
 * Funcionalidades:
 * - Split por valor fixo
 * - Split por percentual
 * - Distribuição de taxas
 * - Validação de recebedores
 * - Logs de auditoria
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class SplitPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(SplitPaymentService.class);

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Processa split de pagamento no PagSeguro
     * 
     * @param gateway Gateway PagSeguro
     * @param transacao Transação original
     * @param splitRequest Configuração do split
     * @return Resultado do processamento
     */
    public Map<String, Object> processPagSeguroSplit(Gateway gateway, Transacao transacao, SplitRequest splitRequest) {
        logger.info("[SPLIT] Processando split PagSeguro - TransactionID: {}", transacao.getTransactionId());

        try {
            // Validar split
            validateSplit(splitRequest, transacao.getAmount());

            // Construir payload
            Map<String, Object> payload = buildPagSeguroSplitPayload(splitRequest, transacao);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição
            String url = gateway.getApiUrl() + "/charges/" + transacao.getGatewayTransactionId() + "/splits";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("[SPLIT] Split PagSeguro processado com sucesso");
                return response.getBody();
            } else {
                throw new RuntimeException("Falha ao processar split");
            }

        } catch (Exception e) {
            logger.error("[SPLIT] Erro ao processar split PagSeguro", e);
            throw new RuntimeException("Erro no split de pagamento", e);
        }
    }

    /**
     * Processa split de pagamento no Mercado Pago (Marketplace)
     * 
     * @param gateway Gateway Mercado Pago
     * @param transacao Transação original
     * @param splitRequest Configuração do split
     * @return Resultado do processamento
     */
    public Map<String, Object> processMercadoPagoSplit(Gateway gateway, Transacao transacao, SplitRequest splitRequest) {
        logger.info("[SPLIT] Processando split Mercado Pago - TransactionID: {}", transacao.getTransactionId());

        try {
            validateSplit(splitRequest, transacao.getAmount());

            Map<String, Object> payload = buildMercadoPagoSplitPayload(splitRequest, transacao);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/v1/payments/" + transacao.getGatewayTransactionId() + "/disbursements";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("[SPLIT] Split Mercado Pago processado com sucesso");
                return response.getBody();
            } else {
                throw new RuntimeException("Falha ao processar split");
            }

        } catch (Exception e) {
            logger.error("[SPLIT] Erro ao processar split Mercado Pago", e);
            throw new RuntimeException("Erro no split de pagamento", e);
        }
    }

    /**
     * Valida configuração de split
     */
    private void validateSplit(SplitRequest splitRequest, Double totalAmount) {
        if (splitRequest.getReceivers() == null || splitRequest.getReceivers().isEmpty()) {
            throw new IllegalArgumentException("Nenhum recebedor definido");
        }

        double totalSplit = 0.0;

        for (SplitRequest.SplitReceiver receiver : splitRequest.getReceivers()) {
            if (receiver.getReceiverId() == null || receiver.getReceiverId().isEmpty()) {
                throw new IllegalArgumentException("Receiver ID obrigatório");
            }

            if (receiver.getAmount() == null || receiver.getAmount() <= 0) {
                throw new IllegalArgumentException("Valor do split inválido");
            }

            totalSplit += receiver.getAmount();
        }

        // Validar se o total do split não excede o valor da transação
        if ("FIXED".equals(splitRequest.getSplitType()) && totalSplit > totalAmount) {
            throw new IllegalArgumentException("Total do split excede o valor da transação");
        }

        if ("PERCENTAGE".equals(splitRequest.getSplitType()) && totalSplit > 100) {
            throw new IllegalArgumentException("Total de percentuais excede 100%");
        }
    }

    /**
     * Constrói payload para split do PagSeguro
     */
    private Map<String, Object> buildPagSeguroSplitPayload(SplitRequest splitRequest, Transacao transacao) {
        Map<String, Object> payload = new HashMap<>();
        
        List<Map<String, Object>> splits = new ArrayList<>();
        
        for (SplitRequest.SplitReceiver receiver : splitRequest.getReceivers()) {
            Map<String, Object> split = new HashMap<>();
            split.put("receiver_id", receiver.getReceiverId());
            split.put("amount", calculateAmount(receiver.getAmount(), transacao.getAmount(), splitRequest.getSplitType()));
            split.put("charge_processing_fee", receiver.isChargeFee());
            splits.add(split);
        }
        
        payload.put("splits", splits);
        payload.put("split_type", splitRequest.getSplitType());
        
        return payload;
    }

    /**
     * Constrói payload para split do Mercado Pago
     */
    private Map<String, Object> buildMercadoPagoSplitPayload(SplitRequest splitRequest, Transacao transacao) {
        Map<String, Object> payload = new HashMap<>();
        
        List<Map<String, Object>> disbursements = new ArrayList<>();
        
        for (SplitRequest.SplitReceiver receiver : splitRequest.getReceivers()) {
            Map<String, Object> disbursement = new HashMap<>();
            disbursement.put("collector_id", receiver.getReceiverId());
            disbursement.put("amount", calculateAmount(receiver.getAmount(), transacao.getAmount(), splitRequest.getSplitType()));
            disbursement.put("application_fee", receiver.isChargeFee() ? 0 : null);
            disbursements.add(disbursement);
        }
        
        payload.put("disbursements", disbursements);
        
        return payload;
    }

    /**
     * Calcula valor baseado no tipo de split
     */
    private double calculateAmount(Double amount, Double totalAmount, String splitType) {
        if ("PERCENTAGE".equals(splitType)) {
            return (totalAmount * amount) / 100.0;
        } else {
            return amount;
        }
    }
}
