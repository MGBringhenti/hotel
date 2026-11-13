package br.com.best2bee.cliente_service.dto;

public record DestinatarioDTO(
        Integer id,
        String nome,
        String email,
        boolean ativo
) {
}