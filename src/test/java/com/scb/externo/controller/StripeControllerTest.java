package com.scb.externo.controller;

import com.scb.externo.service.ExternoService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StripeController.class)
class StripeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ExternoService service;

    @Test
    void webhook_quandoAssinaturaInvalida_deveRetornarBadRequest() throws Exception {
        // não mocka Webhook, pois lança SignatureVerificationException
        mvc.perform(post("/stripe/webhook").contentType(MediaType.APPLICATION_JSON).content("{}").header("Stripe-Signature", "assinatura_errada")).andExpect(status().isBadRequest()).andExpect(content().string("Assinatura inválida"));

        verifyNoInteractions(service);
    }

    @Test
    void webhook_PaymentSucceeded_ChamaMarcarComoPago() throws Exception {
        String payload = """
                {
                  "type": "payment_intent.succeeded",
                  "data": { "object": { "id": "pi_test_123" } }
                }
                """;

        // Fake event
        Event eventMock = mock(Event.class);
        when(eventMock.getType()).thenReturn("payment_intent.succeeded");

        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_test_123");

        EventDataObjectDeserializer deser = mock(EventDataObjectDeserializer.class);
        when(deser.getObject()).thenReturn(Optional.of(pi));
        when(eventMock.getDataObjectDeserializer()).thenReturn(deser);

        try (MockedStatic<Webhook> webhook = Mockito.mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(eventMock);

            mvc.perform(post("/stripe/webhook").contentType(MediaType.APPLICATION_JSON).content(payload).header("Stripe-Signature", "qualquer"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("ok"));
        }

        verify(service).marcarComoPagoPorGatewayId("pi_test_123");
        verify(service, never()).marcarComoFalhaPorGatewayId(anyString());
    }

    @Test
    void webhook_PaymentFailed_ChamaMarcarComoFalha() throws Exception {
        String payload = """
                {
                  "type": "payment_intent.payment_failed",
                  "data": { "object": { "id": "pi_fail_999" } }
                }
                """;

        Event eventMock = mock(Event.class);
        when(eventMock.getType()).thenReturn("payment_intent.payment_failed");

        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_fail_999");

        EventDataObjectDeserializer deser = mock(EventDataObjectDeserializer.class);
        when(deser.getObject()).thenReturn(Optional.of(pi));
        when(eventMock.getDataObjectDeserializer()).thenReturn(deser);

        try (MockedStatic<Webhook> webhook = Mockito.mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(eventMock);

            mvc.perform(post("/stripe/webhook").contentType(MediaType.APPLICATION_JSON).content(payload).header("Stripe-Signature", "qualquer"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("ok"));
        }

        verify(service).marcarComoFalhaPorGatewayId("pi_fail_999");
        verify(service, never()).marcarComoPagoPorGatewayId(anyString());
    }
}
