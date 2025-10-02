package com.pip.gateway;

import com.pip.dto.AuthorizationRequest;
import com.pip.dto.CaptureRequest;
import com.pip.dto.VoidRequest;
import com.pip.dto.PaymentResponse;
import com.pip.model.Gateway;
import com.pip.model.Transacao;

/**
 * Interface base para adaptadores de gateways de pagamento
 * 
 * Define o contrato que todos os adaptadores de gateway devem implementar.
 * Cada gateway (Cielo, Rede, Stone, etc.) terá sua própria implementação.
 * 
 * @author Luiz Gustavo Finotello
 */
public interface GatewayAdapter {

    /**
     * Autoriza um pagamento no gateway
     * 
     * @param gateway Configuração do gateway
     * @param request Dados da autorização
     * @param transacao Transação sendo processada
     * @return Resposta do gateway
     */
    PaymentResponse authorize(Gateway gateway, AuthorizationRequest request, Transacao transacao);

    /**
     * Captura um pagamento previamente autorizado
     * 
     * @param gateway Configuração do gateway
     * @param request Dados da captura
     * @param transacao Transação sendo capturada
     * @return Resposta do gateway
     */
    PaymentResponse capture(Gateway gateway, CaptureRequest request, Transacao transacao);

    /**
     * Cancela um pagamento autorizado
     * 
     * @param gateway Configuração do gateway
     * @param request Dados do cancelamento
     * @param transacao Transação sendo cancelada
     * @return Resposta do gateway
     */
    PaymentResponse voidTransaction(Gateway gateway, VoidRequest request, Transacao transacao);

    /**
     * Verifica saúde do gateway
     * 
     * @param gateway Configuração do gateway
     * @return true se o gateway está saudável, false caso contrário
     */
    boolean healthCheck(Gateway gateway);

    /**
     * Retorna o código do gateway suportado por este adaptador
     * 
     * @return Código do gateway (ex: "CIELO", "REDE", "STONE")
     */
    String getGatewayCode();
}
