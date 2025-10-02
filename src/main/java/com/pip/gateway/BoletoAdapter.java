package com.pip.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pip.dto.AuthorizationRequest;
import com.pip.dto.CaptureRequest;
import com.pip.dto.VoidRequest;
import com.pip.dto.PaymentResponse;
import com.pip.model.Gateway;
import com.pip.model.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador para integração com Boleto Bancário
 * 
 * Implementa comunicação com APIs bancárias para:
 * - Geração de boletos
 * - Consulta de status de pagamento
 * - Cancelamento de boletos
 * 
 * Suporta múltiplos bancos através de configuração
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class BoletoAdapter implements GatewayAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BoletoAdapter.class);
    private static final String GATEWAY_CODE = "BOLETO";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao) {
        logger.info("Iniciando geração de boleto para transação: {}", transacao.getTransactionId());

        try {
            // Calcular data de vencimento (padrão: 3 dias úteis)
            LocalDate dataVencimento = LocalDate.now().plusDays(3);

            // Construir payload do boleto
            Map<String, Object> payload = new HashMap<>();
            payload.put("numeroDocumento", transacao.getTransactionId());
            payload.put("dataVencimento", dataVencimento.format(DateTimeFormatter.ISO_LOCAL_DATE));
            payload.put("valor", request.getAmount());
            payload.put("descricao", request.getDescription());

            // Dados do pagador
            if (request.getCustomer() != null) {
                Map<String, Object> pagador = new HashMap<>();
                pagador.put("nome", request.getCustomer().get("name"));
                pagador.put("cpfCnpj", request.getCustomer().get("document"));
                pagador.put("email", request.getCustomer().get("email"));
                
                // Endereço (se disponível)
                if (request.getCustomer().containsKey("address")) {
                    Map<String, Object> address = (Map<String, Object>) request.getCustomer().get("address");
                    pagador.put("endereco", address);
                }
                
                payload.put("pagador", pagador);
            }

            // Dados do beneficiário (lojista)
            Map<String, Object> beneficiario = new HashMap<>();
            beneficiario.put("agencia", gateway.getAgencia());
            beneficiario.put("conta", gateway.getConta());
            beneficiario.put("codigoBeneficiario", gateway.getMerchantId());
            payload.put("beneficiario", beneficiario);

            // Configurações do boleto
            Map<String, Object> configuracoes = new HashMap<>();
            configuracoes.put("multa", Map.of("tipo", "PERCENTUAL", "valor", 2.0)); // 2% de multa
            configuracoes.put("juros", Map.of("tipo", "PERCENTUAL_DIA", "valor", 0.033)); // 1% ao mês
            configuracoes.put("desconto", Map.of("tipo", "VALOR_FIXO", "valor", 0.0));
            payload.put("configuracoes", configuracoes);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição para gerar boleto
            String url = gateway.getApiUrl() + "/v1/boletos";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();

                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("PENDING"); // Boleto começa como pendente
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId((String) responseBody.get("nossoNumero"));
                
                // Informações específicas do boleto
                Map<String, Object> boletoData = new HashMap<>();
                boletoData.put("linhaDigitavel", responseBody.get("linhaDigitavel"));
                boletoData.put("codigoBarras", responseBody.get("codigoBarras"));
                boletoData.put("urlPdf", responseBody.get("urlPdf"));
                boletoData.put("dataVencimento", dataVencimento.toString());
                boletoData.put("nossoNumero", responseBody.get("nossoNumero"));
                paymentResponse.setAdditionalData(boletoData);
                
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Boleto gerado com sucesso: {}", responseBody.get("nossoNumero"));
                return paymentResponse;
            } else {
                return createErrorResponse("BOLETO_GENERATION_FAILED", "Falha na geração do boleto");
            }

        } catch (Exception e) {
            logger.error("Erro na geração do boleto: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao) {
        logger.info("Consultando status do boleto para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Consultar status do boleto
            String url = String.format("%s/v1/boletos/%s",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId()
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");

                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                // Mapear status do boleto
                if ("PAGO".equals(status) || "LIQUIDADO".equals(status)) {
                    paymentResponse.setSuccess(true);
                    paymentResponse.setStatus("CAPTURED");
                    
                    // Adicionar dados do pagamento
                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("dataPagamento", responseBody.get("dataPagamento"));
                    paymentData.put("valorPago", responseBody.get("valorPago"));
                    paymentResponse.setAdditionalData(paymentData);
                    
                    logger.info("Boleto pago com sucesso: {}", transacao.getGatewayTransactionId());
                } else if ("REGISTRADO".equals(status) || "EMITIDO".equals(status)) {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setStatus("PENDING");
                    paymentResponse.setErrorMessage("Boleto ainda não foi pago");
                } else if ("VENCIDO".equals(status)) {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setStatus("EXPIRED");
                    paymentResponse.setErrorMessage("Boleto vencido");
                } else if ("CANCELADO".equals(status)) {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setStatus("VOIDED");
                    paymentResponse.setErrorMessage("Boleto cancelado");
                } else {
                    paymentResponse.setSuccess(false);
                    paymentResponse.setStatus("UNKNOWN");
                    paymentResponse.setErrorMessage("Status desconhecido: " + status);
                }

                return paymentResponse;
            } else {
                return createErrorResponse("BOLETO_QUERY_FAILED", "Falha na consulta do boleto");
            }

        } catch (Exception e) {
            logger.error("Erro na consulta do boleto: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao) {
        logger.info("Iniciando cancelamento de boleto para transação: {}", transacao.getTransactionId());

        try {
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            // Fazer requisição de cancelamento
            String url = String.format("%s/v1/boletos/%s/cancelar",
                gateway.getApiUrl(),
                transacao.getGatewayTransactionId()
            );

            // Payload com motivo do cancelamento
            Map<String, Object> payload = new HashMap<>();
            payload.put("motivo", request.getReason() != null ? request.getReason() : "Cancelamento solicitado");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Processar resposta
            if (response.getStatusCode() == HttpStatus.OK) {
                PaymentResponse paymentResponse = new PaymentResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setStatus("VOIDED");
                paymentResponse.setTransactionId(transacao.getTransactionId());
                paymentResponse.setGatewayTransactionId(transacao.getGatewayTransactionId());
                paymentResponse.setTimestamp(ZonedDateTime.now());

                logger.info("Boleto cancelado com sucesso: {}", transacao.getGatewayTransactionId());
                return paymentResponse;
            } else {
                return createErrorResponse("BOLETO_CANCELLATION_FAILED", "Falha no cancelamento do boleto");
            }

        } catch (Exception e) {
            logger.error("Erro no cancelamento do boleto: {}", e.getMessage(), e);
            return createErrorResponse("GATEWAY_ERROR", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck(Gateway gateway) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + gateway.getMerchantKey());

            String url = gateway.getApiUrl() + "/v1/health";
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            logger.warn("Health check Boleto falhou: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getGatewayCode() {
        return GATEWAY_CODE;
    }

    private PaymentResponse createErrorResponse(String errorCode, String errorMessage) {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        response.setTimestamp(ZonedDateTime.now());
        return response;
    }
}
