package com.pip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pip.model.Lojista;
import com.pip.model.Transacao;
import com.pip.model.WebhookEvent;
import com.pip.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para WebhookService
 * 
 * @author Luiz Gustavo Finotello
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    private Lojista lojista;
    private Transacao transacao;

    @BeforeEach
    void setUp() {
        // Criar lojista de teste
        lojista = new Lojista();
        lojista.setNomeFantasia("Loja Teste");
        lojista.setRazaoSocial("Loja Teste LTDA");
        lojista.setCnpj("12345678901234");
        lojista.setEmail("teste@loja.com");
        lojista.setPlano(com.pip.model.PlanoLojista.BUSINESS);
        lojista.setWebhookUrl("https://loja.com/webhook");
        lojista.setWebhookSecret("secret123");

        // Criar transação de teste
        transacao = new Transacao();
        transacao.setTransactionId("TXN-" + UUID.randomUUID());
        transacao.setLojista(lojista);
        transacao.setValor(10000L);
        transacao.setMoeda("BRL");
        transacao.setParcelas(1);
        transacao.setStatus(com.pip.model.TransactionStatus.AUTHORIZED);
        transacao.setCreatedAt(ZonedDateTime.now());
    }

    @Test
    void testCriarWebhook_ComWebhookConfigurado() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"event\":\"TRANSACTION_AUTHORIZED\"}");
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> {
            WebhookEvent webhook = invocation.getArgument(0);
            webhook.setId(UUID.randomUUID());
            return webhook;
        });

        // Act
        WebhookEvent webhook = webhookService.criarWebhook(lojista, transacao, "TRANSACTION_AUTHORIZED");

        // Assert
        assertNotNull(webhook);
        assertEquals("TRANSACTION_AUTHORIZED", webhook.getEvento());
        assertEquals(lojista.getWebhookUrl(), webhook.getUrl());
        assertNotNull(webhook.getSignature());
        verify(webhookEventRepository, times(1)).save(any(WebhookEvent.class));
    }

    @Test
    void testCriarWebhook_SemWebhookConfigurado() throws Exception {
        // Arrange
        lojista.setWebhookUrl(null);

        // Act
        WebhookEvent webhook = webhookService.criarWebhook(lojista, transacao, "TRANSACTION_AUTHORIZED");

        // Assert
        assertNull(webhook);
        verify(webhookEventRepository, never()).save(any(WebhookEvent.class));
    }

    @Test
    void testEnviarWebhook_Sucesso() {
        // Arrange
        WebhookEvent webhook = new WebhookEvent();
        webhook.setId(UUID.randomUUID());
        webhook.setLojista(lojista);
        webhook.setTransacao(transacao);
        webhook.setEvento("TRANSACTION_AUTHORIZED");
        webhook.setUrl(lojista.getWebhookUrl());
        webhook.setPayload("{\"event\":\"TRANSACTION_AUTHORIZED\"}");
        webhook.setStatus("PENDING");
        webhook.setTentativas(0);
        webhook.setMaxTentativas(5);

        ResponseEntity<String> response = new ResponseEntity<>("{\"status\":\"ok\"}", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(response);
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean enviado = webhookService.enviarWebhook(webhook);

        // Assert
        assertTrue(enviado);
        verify(webhookEventRepository, atLeast(1)).save(any(WebhookEvent.class));
    }

    @Test
    void testEnviarWebhook_MaxTentativasExcedido() {
        // Arrange
        WebhookEvent webhook = new WebhookEvent();
        webhook.setId(UUID.randomUUID());
        webhook.setTentativas(5);
        webhook.setMaxTentativas(5);

        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean enviado = webhookService.enviarWebhook(webhook);

        // Assert
        assertFalse(enviado);
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testVerificarAssinatura() {
        // Act
        String payload = "{\"test\":\"data\"}";
        String secret = "secret123";
        String signature = webhookService.gerarAssinatura(payload, secret);

        // Assert
        assertNotNull(signature);
        assertTrue(webhookService.verificarAssinatura(payload, signature, secret));
        assertFalse(webhookService.verificarAssinatura(payload, "invalid", secret));
    }
}
