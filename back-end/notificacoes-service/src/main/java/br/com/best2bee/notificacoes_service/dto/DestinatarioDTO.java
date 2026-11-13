package br.com.best2bee.notificacoes_service.dto;

public record DestinatarioDTO(
        Integer id,
        String nome,
        String email,
        Boolean ativo
) {
}