package com.scb.externo.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NovoEmail(
    @NotBlank @Email String email,
    @NotBlank String mensagem
) {}
