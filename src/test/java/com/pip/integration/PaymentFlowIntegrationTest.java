package com.pip.integration;

import com.pip.dto.*;
import com.pip.model.*;
import com.pip.repository.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Testes de Integração End-to-End
 * 
 * Testa o fluxo completo de pagamento:
 * 1. Autorização
 * 2. Captura
 * 3. Consulta
 * 4. Cancelamento
 * 
 * Usa Testcontainers para PostgreSQL real
 * Usa WireMock para simular gateways
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("pip_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private LojistaRepository lojistaRepository;

    @Autowired
    private GatewayRepository gatewayRepository;

    private static String transactionId;
    private static String authorizationCode;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    @Order(1)
    @DisplayName("Deve autorizar pagamento com sucesso")
    void testAuthorizePayment() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setAmount(10000L); // R$ 100,00
        request.setCardToken("tok_test_visa_approved");
        request.setCvv("123");
        request.setInstallments(1);

        transactionId = given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", "test_api_key")
                .body(request)
        .when()
                .post("/v1/payments/authorize")
        .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo("AUTHORIZED"))
                .body("transactionId", notNullValue())
                .body("authorizationCode", notNullValue())
                .extract()
                .path("transactionId");

        System.out.println("✅ Autorização bem-sucedida: " + transactionId);
    }

    @Test
    @Order(2)
    @DisplayName("Deve capturar pagamento autorizado")
    void testCapturePayment() {
        CaptureRequest request = new CaptureRequest();
        request.setAmount(10000L);

        given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", "test_api_key")
                .body(request)
        .when()
                .post("/v1/payments/" + transactionId + "/capture")
        .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo("CAPTURED"));

        System.out.println("✅ Captura bem-sucedida: " + transactionId);
    }

    @Test
    @Order(3)
    @DisplayName("Deve consultar transação por ID")
    void testGetPayment() {
        given()
                .header("X-API-Key", "test_api_key")
        .when()
                .get("/v1/payments/" + transactionId)
        .then()
                .statusCode(200)
                .body("transactionId", equalTo(transactionId))
                .body("status", equalTo("CAPTURED"))
                .body("amount", equalTo(10000));

        System.out.println("✅ Consulta bem-sucedida: " + transactionId);
    }

    @Test
    @Order(4)
    @DisplayName("Deve cancelar transação capturada")
    void testVoidPayment() {
        VoidRequest request = new VoidRequest();
        request.setReason("Teste de cancelamento");

        given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", "test_api_key")
                .body(request)
        .when()
                .post("/v1/payments/" + transactionId + "/void")
        .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo("VOIDED"));

        System.out.println("✅ Cancelamento bem-sucedido: " + transactionId);
    }

    @Test
    @Order(5)
    @DisplayName("Deve listar transações com filtros")
    void testListPayments() {
        given()
                .header("X-API-Key", "test_api_key")
                .queryParam("status", "VOIDED")
                .queryParam("page", 0)
                .queryParam("size", 10)
        .when()
                .get("/v1/payments")
        .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("content.size()", greaterThanOrEqualTo(1));

        System.out.println("✅ Listagem bem-sucedida");
    }

    @Test
    @DisplayName("Deve rejeitar pagamento sem API Key")
    void testUnauthorizedAccess() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setAmount(10000L);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/v1/payments/authorize")
        .then()
                .statusCode(401);

        System.out.println("✅ Rejeição de acesso não autorizado funcionando");
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios")
    void testValidation() {
        AuthorizationRequest request = new AuthorizationRequest();
        // Sem amount e cardToken

        given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", "test_api_key")
                .body(request)
        .when()
                .post("/v1/payments/authorize")
        .then()
                .statusCode(400);

        System.out.println("✅ Validação de campos obrigatórios funcionando");
    }
}
