package com.scb.externo.dto;

public record NovoCartaoDeCredito(
        String nomeTitular,
        String numero,
        String validade,
        String cvv
) {}
