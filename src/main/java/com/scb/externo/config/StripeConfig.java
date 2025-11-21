package com.scb.externo.config;

import com.stripe.Stripe;                             // <-- importa Stripe
import jakarta.annotation.PostConstruct;             // <-- importa PostConstruct
import org.springframework.beans.factory.annotation.Value;   // <-- importa Value
import org.springframework.context.annotation.Configuration; // <-- importa Configuration

@Configuration
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println(">>> stripe.api.key NÃO configurada (apiKey está vazia)");
        } else {
            Stripe.apiKey = apiKey;
            System.out.println(">>> Stripe API key carregada com sucesso");
        }
    }
}
