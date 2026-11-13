package br.com.best2bee.notificacoes_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public record EventoKafkaDTO(

    @JsonAlias("eventoID") UUID eventoId,
    String tipo,
    Instant criadoEm,
    ReservaEventoDTO dados
){}
