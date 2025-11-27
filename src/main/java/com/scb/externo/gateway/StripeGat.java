package com.scb.externo.gateway;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StripeGat {

    // Corrigido para StripeGat.class
    private static final Logger log = LoggerFactory.getLogger(StripeGat.class);

    public PaymentIntent criarIntencaoDePagamento(long valorEmCentavos, String descricao) throws StripeException {
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(valorEmCentavos)
                        .setCurrency("brl")
                        .setDescription(descricao)
                        .addPaymentMethodType("card")
                        .build();

        return PaymentIntent.create(params);
    }

    public PaymentIntent confirmarPaymentIntentComCartaoTeste(String paymentIntentId) throws StripeException {
        PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                .setPaymentMethod("pm_card_visa")
                .build();

        PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
        PaymentIntent confirmado = pi.confirm(confirmParams);

        // Substitui o System.out por log
        log.info("PaymentIntent {} confirmado com status={}", confirmado.getId(), confirmado.getStatus());

        return confirmado;
    }
}
