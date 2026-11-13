package br.com.best2bee.reserva_service.dto;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;;

public record EventoKafkaDTO(

    UUID eventoID,
    String tipo,
    Instant criadoEm,
    JsonNode dados
) {}
