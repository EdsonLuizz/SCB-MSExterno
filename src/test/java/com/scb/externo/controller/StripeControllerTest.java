package com.scb.externo.controller;

import com.scb.externo.service.ExternoService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeControllerTest {

    @Mock
    ExternoService service;

    StripeController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new StripeController(service);

        // injeta um webhookSecret "fake", pois @Value não é processado em teste unitário puro
        Field f = StripeController.class.getDeclaredField("webhookSecret");
        f.setAccessible(true);
        f.set(controller, "whsec_test");
    }

    @Test
    void handleWebhook_deveRetornar400QuandoAssinaturaInvalida() {
        String payload = "{ \"id\":\"evt_1\" }";
        String signature = "assinatura_errada";

        // Mock estático do Webhook.constructEvent para lançar SignatureVerificationException
        try (MockedStatic<Webhook> mocked = Mockito.mockStatic(Webhook.class)) {
            mocked.when(() -> Webhook.constructEvent(payload, signature, "whsec_test"))
                    .thenThrow(new SignatureVerificationException("erro", null));

            ResponseEntity<String> resp = controller.handleWebhook(payload, signature);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("Assinatura inválida", resp.getBody());
            verifyNoInteractions(service); // não deve tentar marcar cobrança
        }
    }

    @Test
    void handleWebhook_deveChamarMarcarComoPagoQuandoPaymentIntentSucceeded() {
        String payload = "{ \"id\":\"evt_succeeded\" }";
        String signature = "assinatura_ok";

        Event eventMock = Mockito.mock(Event.class);
        PaymentIntent piMock = Mockito.mock(PaymentIntent.class);
        when(piMock.getId()).thenReturn("pi_123");

        EventDataObjectDeserializer deserMock = Mockito.mock(EventDataObjectDeserializer.class);
        when(deserMock.getObject()).thenReturn(Optional.of(piMock));

        when(eventMock.getType()).thenReturn("payment_intent.succeeded");
        when(eventMock.getDataObjectDeserializer()).thenReturn(deserMock);

        try (MockedStatic<Webhook> mocked = Mockito.mockStatic(Webhook.class)) {
            mocked.when(() -> Webhook.constructEvent(payload, signature, "whsec_test"))
                    .thenReturn(eventMock);

            ResponseEntity<String> resp = controller.handleWebhook(payload, signature);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals("ok", resp.getBody());

            // garante que chamou o service com o ID do PaymentIntent
            verify(service).marcarComoPagoPorGatewayId("pi_123");
            verify(service, never()).marcarComoFalhaPorGatewayId(anyString());
        }
    }

    @Test
    void handleWebhook_deveChamarMarcarComoFalhaQuandoPaymentFailed() {
        String payload = "{ \"id\":\"evt_failed\" }";
        String signature = "assinatura_ok";

        Event eventMock = Mockito.mock(Event.class);
        PaymentIntent piMock = Mockito.mock(PaymentIntent.class);
        when(piMock.getId()).thenReturn("pi_456");

        EventDataObjectDeserializer deserMock = Mockito.mock(EventDataObjectDeserializer.class);
        when(deserMock.getObject()).thenReturn(Optional.of(piMock));

        when(eventMock.getType()).thenReturn("payment_intent.payment_failed");
        when(eventMock.getDataObjectDeserializer()).thenReturn(deserMock);

        try (MockedStatic<Webhook> mocked = Mockito.mockStatic(Webhook.class)) {
            mocked.when(() -> Webhook.constructEvent(payload, signature, "whsec_test"))
                    .thenReturn(eventMock);

            ResponseEntity<String> resp = controller.handleWebhook(payload, signature);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals("ok", resp.getBody());

            verify(service).marcarComoFalhaPorGatewayId("pi_456");
            verify(service, never()).marcarComoPagoPorGatewayId(anyString());
        }
    }
}
