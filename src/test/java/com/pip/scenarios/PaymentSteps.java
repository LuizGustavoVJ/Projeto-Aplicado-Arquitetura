package com.pip.scenarios;

import com.pip.dto.*;
import io.cucumber.java.pt.*;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Step Definitions para testes BDD com Cucumber
 * 
 * Implementa os passos (Given/When/Then) definidos nos arquivos .feature
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaymentSteps {

    @LocalServerPort
    private int port;

    private RequestSpecification request;
    private Response response;
    private String transactionId;
    private String apiKey = "test_api_key";

    @Dado("que o sistema está configurado")
    public void sistemaConfigurado() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Dado("que eu tenho uma API Key válida")
    public void apiKeyValida() {
        request = given()
                .header("X-API-Key", apiKey)
                .contentType("application/json");
    }

    @Dado("que eu tenho um cartão de crédito válido")
    public void cartaoValido() {
        // Cartão de teste que será aprovado
    }

    @Dado("que eu tenho um cartão de crédito inválido")
    public void cartaoInvalido() {
        // Cartão de teste que será rejeitado
    }

    @Dado("que eu tenho um cartão {string}")
    public void cartaoBandeira(String bandeira) {
        // Configura cartão da bandeira específica
    }

    @Dado("que eu tenho um pagamento autorizado")
    public void pagamentoAutorizado() {
        AuthorizationRequest authRequest = new AuthorizationRequest();
        authRequest.setAmount(10000L);
        authRequest.setCardToken("tok_test_visa_approved");
        authRequest.setCvv("123");
        authRequest.setInstallments(1);

        transactionId = given()
                .header("X-API-Key", apiKey)
                .contentType("application/json")
                .body(authRequest)
        .when()
                .post("/v1/payments/authorize")
        .then()
                .statusCode(200)
                .extract()
                .path("transactionId");
    }

    @Dado("que eu tenho um pagamento capturado")
    public void pagamentoCapturado() {
        pagamentoAutorizado();

        CaptureRequest captureRequest = new CaptureRequest();
        captureRequest.setAmount(10000L);

        given()
                .header("X-API-Key", apiKey)
                .contentType("application/json")
                .body(captureRequest)
        .when()
                .post("/v1/payments/" + transactionId + "/capture")
        .then()
                .statusCode(200);
    }

    @Dado("que existem transações no sistema")
    public void transacoesNoSistema() {
        // Criar algumas transações de teste
        pagamentoCapturado();
    }

    @Quando("eu envio uma requisição de autorização de R$ {double}")
    public void autorizarValor(double valor) {
        AuthorizationRequest authRequest = new AuthorizationRequest();
        authRequest.setAmount((long)(valor * 100)); // Converter para centavos
        authRequest.setCardToken("tok_test_visa_approved");
        authRequest.setCvv("123");
        authRequest.setInstallments(1);

        response = request
                .body(authRequest)
        .when()
                .post("/v1/payments/authorize");
    }

    @Quando("eu envio uma requisição de autorização")
    public void autorizarPagamento() {
        autorizarValor(100.00);
    }

    @Quando("eu envio uma requisição de captura")
    public void capturarPagamento() {
        CaptureRequest captureRequest = new CaptureRequest();
        captureRequest.setAmount(10000L);

        response = request
                .body(captureRequest)
        .when()
                .post("/v1/payments/" + transactionId + "/capture");
    }

    @Quando("eu envio uma requisição de cancelamento")
    public void cancelarPagamento() {
        VoidRequest voidRequest = new VoidRequest();
        voidRequest.setReason("Teste de cancelamento");

        response = request
                .body(voidRequest)
        .when()
                .post("/v1/payments/" + transactionId + "/void");
    }

    @Quando("eu envio uma requisição sem o valor")
    public void requisicaoSemValor() {
        AuthorizationRequest authRequest = new AuthorizationRequest();
        // Sem amount
        authRequest.setCardToken("tok_test_visa_approved");

        response = request
                .body(authRequest)
        .when()
                .post("/v1/payments/authorize");
    }

    @Quando("eu consulto transações com status {string}")
    public void consultarTransacoesComStatus(String status) {
        response = request
                .queryParam("status", status)
        .when()
                .get("/v1/payments");
    }

    @Então("o pagamento deve ser autorizado")
    public void pagamentoAutorizadoComSucesso() {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo("AUTHORIZED"));

        transactionId = response.path("transactionId");
    }

    @Então("o pagamento deve ser capturado")
    public void pagamentoCapturadoComSucesso() {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo("CAPTURED"));
    }

    @Então("o pagamento deve ser cancelado")
    public void pagamentoCanceladoComSucesso() {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo("VOIDED"));
    }

    @Então("o pagamento deve ser rejeitado")
    public void pagamentoRejeitado() {
        response.then()
                .body("success", equalTo(false));
    }

    @Então("eu devo receber um ID de transação")
    public void receberIdTransacao() {
        response.then()
                .body("transactionId", notNullValue());
    }

    @Então("o status deve ser {string}")
    public void verificarStatus(String status) {
        response.then()
                .body("status", equalTo(status));
    }

    @Então("eu devo receber uma mensagem de erro")
    public void receberMensagemErro() {
        response.then()
                .body("errorMessage", notNullValue());
    }

    @Então("eu devo receber um erro de validação")
    public void receberErroValidacao() {
        response.then()
                .statusCode(400);
    }

    @Então("o status HTTP deve ser {int}")
    public void verificarStatusHttp(int statusCode) {
        response.then()
                .statusCode(statusCode);
    }

    @Então("eu devo receber uma lista de transações")
    public void receberListaTransacoes() {
        response.then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Então("todas devem ter status {string}")
    public void todasComStatus(String status) {
        response.then()
                .body("content.every { it.status == '" + status + "' }", equalTo(true));
    }

    @Então("a bandeira deve ser identificada como {string}")
    public void verificarBandeira(String bandeira) {
        response.then()
                .body("cardBrand", equalToIgnoringCase(bandeira));
    }
}
