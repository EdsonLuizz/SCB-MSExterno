package com.scb.externo.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${STRIPE_API_KEY:}")
    private String apiKey;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        alteracampoStripe(apiKey);
    }

    // método alterar o campo estático da Stripe
    static void alteracampoStripe(String apiKey) {
        Stripe.apiKey = apiKey;
    }
}
