package com.pip.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pip.model.Lojista;
import com.pip.repository.LojistaRepository;
import com.pip.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Interceptor para aplicar Rate Limiting nas requisições
 * 
 * Verifica se a API Key está dentro do limite de requisições permitido
 * e adiciona headers informativos na resposta.
 * 
 * @author Luiz Gustavo Finotello
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private LojistaRepository lojistaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Ignorar requisições de health check e documentação
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/api-docs")) {
            return true;
        }

        // Obter API Key do header
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // Se não tem API Key, não aplicar rate limiting
            return true;
        }

        try {
            // Buscar lojista
            Optional<Lojista> lojistaOpt = lojistaRepository.findByApiKey(apiKey);
            
            if (lojistaOpt.isEmpty()) {
                // API Key inválida - deixar o controller tratar
                return true;
            }

            Lojista lojista = lojistaOpt.get();
            
            // Obter limite baseado no plano
            int maxRequests = rateLimitService.getLimitByPlan(lojista.getPlano().name());
            
            // Verificar rate limit
            boolean allowed = rateLimitService.isAllowed(apiKey, maxRequests);
            
            // Adicionar headers informativos
            long remaining = rateLimitService.getRemainingRequests(apiKey, maxRequests);
            long resetTime = rateLimitService.getResetTime(apiKey);
            
            response.setHeader(RATE_LIMIT_HEADER, String.valueOf(maxRequests));
            response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));
            response.setHeader(RATE_LIMIT_RESET_HEADER, String.valueOf(System.currentTimeMillis() / 1000 + resetTime));
            
            if (!allowed) {
                // Rate limit excedido
                logger.warn("Rate limit excedido para lojista: {} ({})", lojista.getNomeFantasia(), apiKey);
                
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "RATE_LIMIT_EXCEEDED");
                errorResponse.put("message", "Limite de requisições excedido. Tente novamente em " + resetTime + " segundos.");
                errorResponse.put("limit", maxRequests);
                errorResponse.put("remaining", 0);
                errorResponse.put("resetIn", resetTime);
                
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                
                return false;
            }
            
            logger.debug("Rate limit OK para lojista: {} - Restantes: {}/{}", 
                lojista.getNomeFantasia(), remaining, maxRequests);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Erro ao verificar rate limit: {}", e.getMessage(), e);
            // Em caso de erro, permitir requisição
            return true;
        }
    }
}
