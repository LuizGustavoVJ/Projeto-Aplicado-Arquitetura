package com.pip.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory para gerenciar e fornecer adaptadores de gateway
 * 
 * Registra automaticamente todos os adaptadores disponíveis e fornece
 * acesso aos adaptadores baseado no código do gateway.
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class GatewayAdapterFactory {

    private static final Logger logger = LoggerFactory.getLogger(GatewayAdapterFactory.class);

    private final Map<String, GatewayAdapter> adapters = new HashMap<>();

    /**
     * Construtor que registra automaticamente todos os adaptadores disponíveis
     * 
     * @param adapterList Lista de adaptadores injetados pelo Spring
     */
    @Autowired
    public GatewayAdapterFactory(List<GatewayAdapter> adapterList) {
        for (GatewayAdapter adapter : adapterList) {
            String code = adapter.getGatewayCode();
            adapters.put(code, adapter);
            logger.info("Adaptador registrado: {}", code);
        }
        
        logger.info("Total de adaptadores registrados: {}", adapters.size());
    }

    /**
     * Obtém o adaptador para um gateway específico
     * 
     * @param gatewayCode Código do gateway (ex: "CIELO", "REDE", "PIX")
     * @return Adaptador correspondente
     * @throws IllegalArgumentException se o adaptador não for encontrado
     */
    public GatewayAdapter getAdapter(String gatewayCode) {
        if (gatewayCode == null || gatewayCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Código do gateway não pode ser vazio");
        }

        String normalizedCode = gatewayCode.toUpperCase().trim();
        GatewayAdapter adapter = adapters.get(normalizedCode);

        if (adapter == null) {
            logger.error("Adaptador não encontrado para gateway: {}", gatewayCode);
            throw new IllegalArgumentException("Adaptador não encontrado para gateway: " + gatewayCode);
        }

        return adapter;
    }

    /**
     * Verifica se existe um adaptador para o gateway especificado
     * 
     * @param gatewayCode Código do gateway
     * @return true se o adaptador existe, false caso contrário
     */
    public boolean hasAdapter(String gatewayCode) {
        if (gatewayCode == null || gatewayCode.trim().isEmpty()) {
            return false;
        }

        String normalizedCode = gatewayCode.toUpperCase().trim();
        return adapters.containsKey(normalizedCode);
    }

    /**
     * Retorna a lista de códigos de gateways suportados
     * 
     * @return Lista de códigos de gateways
     */
    public List<String> getSupportedGateways() {
        return List.copyOf(adapters.keySet());
    }

    /**
     * Retorna o número de adaptadores registrados
     * 
     * @return Número de adaptadores
     */
    public int getAdapterCount() {
        return adapters.size();
    }
}
