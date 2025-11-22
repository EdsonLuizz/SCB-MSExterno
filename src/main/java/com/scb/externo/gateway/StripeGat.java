package com.scb.externo.gateway;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import org.springframework.stereotype.Service;

@Service
public class StripeGat {

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
        // Aqui usamos um PaymentMethod de teste já pronto da Stripe
        PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                .setPaymentMethod("pm_card_visa")   // sempre o mesmo “cartão de teste”
                .build();

        PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
        PaymentIntent confirmado = pi.confirm(confirmParams);

        System.out.println(">>> PaymentIntent " + confirmado.getId()
                + " status = " + confirmado.getStatus());

        return confirmado;
    }
}
