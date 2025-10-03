package com.pip.stress;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Teste de Stress/Carga com Gatling
 * 
 * Simula carga real de produção:
 * - 100 usuários simultâneos
 * - 1000 transações por minuto
 * - Duração: 5 minutos
 * 
 * Métricas coletadas:
 * - Tempo de resposta (p50, p95, p99)
 * - Taxa de sucesso
 * - Throughput
 * - Erros
 */
public class PaymentStressTest extends Simulation {

    // Configuração HTTP
    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .header("X-API-Key", "test_api_key");

    // Cenário 1: Autorização de pagamento
    ScenarioBuilder authorizeScenario = scenario("Authorize Payment")
            .exec(http("Authorize")
                    .post("/v1/payments/authorize")
                    .body(StringBody("""
                            {
                                "amount": 10000,
                                "cardToken": "tok_test_visa_approved",
                                "cvv": "123",
                                "installments": 1
                            }
                            """))
                    .check(status().is(200))
                    .check(jsonPath("$.success").is("true"))
                    .check(jsonPath("$.transactionId").saveAs("transactionId"))
            );

    // Cenário 2: Captura de pagamento
    ScenarioBuilder captureScenario = scenario("Capture Payment")
            .exec(http("Authorize")
                    .post("/v1/payments/authorize")
                    .body(StringBody("""
                            {
                                "amount": 10000,
                                "cardToken": "tok_test_visa_approved",
                                "cvv": "123",
                                "installments": 1
                            }
                            """))
                    .check(jsonPath("$.transactionId").saveAs("transactionId"))
            )
            .pause(1)
            .exec(http("Capture")
                    .post("/v1/payments/#{transactionId}/capture")
                    .body(StringBody("""
                            {
                                "amount": 10000
                            }
                            """))
                    .check(status().is(200))
            );

    // Cenário 3: Consulta de transação
    ScenarioBuilder getScenario = scenario("Get Payment")
            .exec(http("Get Payment")
                    .get("/v1/payments/#{transactionId}")
                    .check(status().is(200))
            );

    // Cenário 4: Listagem com filtros
    ScenarioBuilder listScenario = scenario("List Payments")
            .exec(http("List")
                    .get("/v1/payments")
                    .queryParam("page", "0")
                    .queryParam("size", "20")
                    .check(status().is(200))
            );

    // Configuração de carga
    {
        setUp(
                // 50 usuários fazendo autorizações
                authorizeScenario.injectOpen(
                        rampUsersPerSec(10).to(50).during(60),
                        constantUsersPerSec(50).during(240)
                ),
                // 30 usuários fazendo capturas
                captureScenario.injectOpen(
                        rampUsersPerSec(5).to(30).during(60),
                        constantUsersPerSec(30).during(240)
                ),
                // 10 usuários consultando
                getScenario.injectOpen(
                        rampUsersPerSec(2).to(10).during(60),
                        constantUsersPerSec(10).during(240)
                ),
                // 10 usuários listando
                listScenario.injectOpen(
                        rampUsersPerSec(2).to(10).during(60),
                        constantUsersPerSec(10).during(240)
                )
        ).protocols(httpProtocol)
         .assertions(
                 // 95% das requisições devem ter sucesso
                 global().successfulRequests().percent().gte(95.0),
                 // 95% das requisições devem responder em menos de 2s
                 global().responseTime().percentile3().lt(2000),
                 // 99% das requisições devem responder em menos de 5s
                 global().responseTime().percentile4().lt(5000)
         );
    }
}
