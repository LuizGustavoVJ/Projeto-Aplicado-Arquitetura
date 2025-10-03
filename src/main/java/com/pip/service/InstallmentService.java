package com.pip.service;

import com.pip.model.Gateway;
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
 * Serviço de Parcelamento Inteligente
 * 
 * Calcula opções de parcelamento com ou sem juros
 * Integrado com Mercado Pago e outros gateways
 * 
 * Funcionalidades:
 * - Cálculo de parcelas com juros
 * - Parcelamento sem juros
 * - Taxas por bandeira
 * - Valor mínimo por parcela
 * - Recomendação de melhor opção
 * 
 * @author Luiz Gustavo Finotello
 */
@Service
public class InstallmentService {

    private static final Logger logger = LoggerFactory.getLogger(InstallmentService.class);
    private static final Double MIN_INSTALLMENT_VALUE = 5.0; // R$ 5,00 mínimo por parcela

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Calcula opções de parcelamento disponíveis
     * 
     * @param gateway Gateway de pagamento
     * @param amount Valor total
     * @param maxInstallments Número máximo de parcelas
     * @param interestFree Parcelas sem juros
     * @return Lista de opções de parcelamento
     */
    public List<InstallmentOption> calculateInstallments(
            Gateway gateway,
            Double amount,
            Integer maxInstallments,
            Integer interestFree) {

        logger.info("[PARCELAMENTO] Calculando opções - Valor: R$ {} - Max: {}x - Sem juros: {}x",
            amount, maxInstallments, interestFree);

        List<InstallmentOption> options = new ArrayList<>();

        // Validar parâmetros
        if (maxInstallments == null || maxInstallments < 1) {
            maxInstallments = 12; // Padrão 12x
        }

        if (interestFree == null || interestFree < 0) {
            interestFree = 0; // Sem parcelas sem juros por padrão
        }

        // Calcular opções
        for (int i = 1; i <= maxInstallments; i++) {
            Double installmentValue;
            Double totalAmount;
            Double interestRate = 0.0;

            if (i <= interestFree) {
                // Sem juros
                installmentValue = amount / i;
                totalAmount = amount;
            } else {
                // Com juros (taxa média de 2.99% ao mês)
                interestRate = 2.99;
                totalAmount = calculateTotalWithInterest(amount, i, interestRate);
                installmentValue = totalAmount / i;
            }

            // Verificar valor mínimo por parcela
            if (installmentValue < MIN_INSTALLMENT_VALUE) {
                logger.debug("[PARCELAMENTO] Parcela {}x abaixo do mínimo: R$ {}", i, installmentValue);
                break;
            }

            InstallmentOption option = new InstallmentOption();
            option.setInstallments(i);
            option.setInstallmentValue(installmentValue);
            option.setTotalAmount(totalAmount);
            option.setInterestRate(interestRate);
            option.setInterestFree(i <= interestFree);
            option.setRecommended(i == interestFree || (interestFree == 0 && i == 1));

            options.add(option);
        }

        logger.info("[PARCELAMENTO] {} opções calculadas", options.size());
        return options;
    }

    /**
     * Calcula valor total com juros compostos
     */
    private Double calculateTotalWithInterest(Double amount, Integer installments, Double monthlyRate) {
        double rate = monthlyRate / 100.0;
        double factor = Math.pow(1 + rate, installments);
        return amount * factor;
    }

    /**
     * Busca opções de parcelamento do Mercado Pago
     * 
     * @param gateway Gateway Mercado Pago
     * @param amount Valor
     * @param paymentMethodId Método de pagamento
     * @return Opções de parcelamento
     */
    public List<InstallmentOption> getMercadoPagoInstallments(
            Gateway gateway,
            Double amount,
            String paymentMethodId) {

        logger.info("[PARCELAMENTO] Buscando opções Mercado Pago - Valor: R$ {}", amount);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = String.format(
                "%s/v1/payment_methods/installments?amount=%.2f&payment_method_id=%s",
                gateway.getApiUrl(),
                amount,
                paymentMethodId != null ? paymentMethodId : "credit_card"
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                List.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> mpOptions = response.getBody();
                return convertMercadoPagoOptions(mpOptions);
            }

        } catch (Exception e) {
            logger.error("[PARCELAMENTO] Erro ao buscar opções Mercado Pago", e);
        }

        // Fallback: calcular localmente
        return calculateInstallments(gateway, amount, 12, 3);
    }

    /**
     * Converte opções do Mercado Pago para formato padrão
     */
    private List<InstallmentOption> convertMercadoPagoOptions(List<Map<String, Object>> mpOptions) {
        List<InstallmentOption> options = new ArrayList<>();

        if (mpOptions != null && !mpOptions.isEmpty()) {
            Map<String, Object> payerCosts = mpOptions.get(0);
            List<Map<String, Object>> installments = (List<Map<String, Object>>) payerCosts.get("payer_costs");

            for (Map<String, Object> inst : installments) {
                InstallmentOption option = new InstallmentOption();
                option.setInstallments((Integer) inst.get("installments"));
                option.setInstallmentValue((Double) inst.get("installment_amount"));
                option.setTotalAmount((Double) inst.get("total_amount"));
                option.setInterestRate((Double) inst.get("installment_rate"));
                option.setInterestFree((Double) inst.get("installment_rate") == 0.0);
                option.setRecommended((Boolean) inst.get("recommended"));

                options.add(option);
            }
        }

        return options;
    }

    /**
     * Classe para opção de parcelamento
     */
    public static class InstallmentOption {
        private Integer installments;
        private Double installmentValue;
        private Double totalAmount;
        private Double interestRate;
        private Boolean interestFree;
        private Boolean recommended;

        // Getters e Setters
        public Integer getInstallments() { return installments; }
        public void setInstallments(Integer installments) { this.installments = installments; }

        public Double getInstallmentValue() { return installmentValue; }
        public void setInstallmentValue(Double installmentValue) { this.installmentValue = installmentValue; }

        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

        public Double getInterestRate() { return interestRate; }
        public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }

        public Boolean getInterestFree() { return interestFree; }
        public void setInterestFree(Boolean interestFree) { this.interestFree = interestFree; }

        public Boolean getRecommended() { return recommended; }
        public void setRecommended(Boolean recommended) { this.recommended = recommended; }
    }
}
