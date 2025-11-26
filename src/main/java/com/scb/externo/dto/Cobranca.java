package com.scb.externo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public record Cobranca(Long id, String status,

                       @JsonFormat (
                               shape = JsonFormat.Shape.STRING,
                               pattern = "dd-MM-yyyy'T'HH:mm:ssXXX",
                               timezone = "America/Sao_Paulo"
                       ) Instant horaSolicitacao,

                       @JsonFormat(
                               shape = JsonFormat.Shape.STRING,
                               pattern = "dd-MM-yyyy'T'HH:mm:ssXXX",
                               timezone = "America/Sao_Paulo"
                       )Instant horaFinalizacao,

                       Long valor, String ciclista,

                       @JsonIgnore
                       String gatewayID) {}
