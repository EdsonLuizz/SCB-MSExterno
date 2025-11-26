package com.scb.externo.controller;

import com.scb.externo.service.ExternoService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/stripe")
public class StripeController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final ExternoService service;

    public StripeController(ExternoService service) {
        this.service = service;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Assinatura inválida");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent pi = extractPaymentIntent(event).orElse(null);
            if (pi != null) {
                service.marcarComoPagoPorGatewayId(pi.getId());
            }
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            PaymentIntent pi = extractPaymentIntent(event).orElse(null);
            if (pi != null) {
                service.marcarComoFalhaPorGatewayId(pi.getId());
            }
        }

        return ResponseEntity.ok("ok");
    }

    private Optional<PaymentIntent> extractPaymentIntent(Event event) {
        EventDataObjectDeserializer deser = event.getDataObjectDeserializer();
        return deser.getObject().map(obj -> (PaymentIntent) obj);
    }
}
