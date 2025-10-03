package com.pip.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de Testes de Segurança
 * 
 * Implementa testes básicos de penetration testing
 * e validação de conformidade PCI-DSS
 * 
 * Testes incluídos:
 * - SQL Injection
 * - XSS (Cross-Site Scripting)
 * - CSRF (Cross-Site Request Forgery)
 * - Autenticação e Autorização
 * - Rate Limiting
 * - Criptografia de dados sensíveis
 * - Validação de entrada
 * - Headers de segurança
 * 
 * @author Luiz Gustavo Finotello
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SecurityTestSuite {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Test 1: SQL Injection Protection")
    public void testSQLInjectionProtection() {
        // Tentar SQL injection no endpoint de pagamento
        String maliciousPayload = "'; DROP TABLE transacao; --";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String body = String.format("{\"amount\": %s}", maliciousPayload);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/payments/authorize",
            request,
            String.class
        );
        
        // Deve retornar erro de validação, não executar SQL
        assertTrue(response.getStatusCode().is4xxClientError() || 
                   response.getStatusCode().is5xxServerError());
        assertFalse(response.getBody().contains("SQL"));
    }

    @Test
    @DisplayName("Test 2: XSS Protection")
    public void testXSSProtection() {
        // Tentar XSS no campo de descrição
        String xssPayload = "<script>alert('XSS')</script>";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String body = String.format("{\"description\": \"%s\"}", xssPayload);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/payments/authorize",
            request,
            String.class
        );
        
        // Script deve ser sanitizado
        if (response.getBody() != null) {
            assertFalse(response.getBody().contains("<script>"));
        }
    }

    @Test
    @DisplayName("Test 3: Authentication Required")
    public void testAuthenticationRequired() {
        // Tentar acessar endpoint sem autenticação
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/payments/12345",
            String.class
        );
        
        // Deve retornar 401 Unauthorized
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Test 4: Rate Limiting")
    public void testRateLimiting() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "test-key");
        
        int successCount = 0;
        int rateLimitCount = 0;
        
        // Fazer 100 requisições rápidas
        for (int i = 0; i < 100; i++) {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                successCount++;
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                rateLimitCount++;
            }
        }
        
        // Deve ter bloqueado algumas requisições
        assertTrue(rateLimitCount > 0, "Rate limiting não está funcionando");
    }

    @Test
    @DisplayName("Test 5: Sensitive Data Encryption")
    public void testSensitiveDataEncryption() {
        // Verificar se dados sensíveis não são retornados em texto plano
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "valid-key");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/payments/12345",
            HttpMethod.GET,
            request,
            String.class
        );
        
        if (response.getBody() != null) {
            // Não deve conter número de cartão completo
            assertFalse(response.getBody().matches(".*\\d{16}.*"));
            // Não deve conter CVV
            assertFalse(response.getBody().contains("\"cvv\""));
        }
    }

    @Test
    @DisplayName("Test 6: HTTPS Required")
    public void testHTTPSRequired() {
        // Verificar se headers de segurança estão presentes
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/health",
            String.class
        );
        
        HttpHeaders headers = response.getHeaders();
        
        // Verificar Strict-Transport-Security
        assertTrue(headers.containsKey("Strict-Transport-Security") ||
                   headers.containsKey("strict-transport-security"));
    }

    @Test
    @DisplayName("Test 7: Input Validation")
    public void testInputValidation() {
        // Testar validação de entrada com dados inválidos
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Valor negativo
        String body = "{\"amount\": -100}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/payments/authorize",
            request,
            String.class
        );
        
        // Deve retornar erro de validação
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Test 8: CORS Configuration")
    public void testCORSConfiguration() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://malicious-site.com");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/payments",
            HttpMethod.OPTIONS,
            request,
            String.class
        );
        
        // CORS deve estar configurado corretamente
        HttpHeaders responseHeaders = response.getHeaders();
        if (responseHeaders.containsKey("Access-Control-Allow-Origin")) {
            String allowedOrigin = responseHeaders.getFirst("Access-Control-Allow-Origin");
            // Não deve permitir qualquer origem
            assertNotEquals("*", allowedOrigin);
        }
    }

    @Test
    @DisplayName("Test 9: Error Message Information Disclosure")
    public void testErrorMessageSecurity() {
        // Tentar causar erro e verificar se mensagem não expõe informações sensíveis
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/payments/invalid-id",
            String.class
        );
        
        if (response.getBody() != null) {
            String body = response.getBody().toLowerCase();
            // Não deve expor stack trace
            assertFalse(body.contains("exception"));
            assertFalse(body.contains("stacktrace"));
            // Não deve expor caminhos de arquivo
            assertFalse(body.contains("/home/"));
            assertFalse(body.contains("c:\\"));
        }
    }

    @Test
    @DisplayName("Test 10: PCI-DSS Compliance - No Card Data Storage")
    public void testPCIDSSCompliance() {
        // Verificar que dados de cartão não são armazenados
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "valid-key");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/payments",
            HttpMethod.GET,
            request,
            String.class
        );
        
        if (response.getBody() != null) {
            String body = response.getBody();
            // Não deve conter PAN completo
            assertFalse(body.matches(".*\\d{13,19}.*"));
            // Não deve conter CVV
            assertFalse(body.contains("cvv"));
            // Deve usar tokens
            assertTrue(body.contains("token") || body.isEmpty());
        }
    }
}
