package br.com.best2bee.cliente_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AlterarSituacaoUsuarioDTO(

    @NotNull (message = "Deve informar se o usuário deve estar ativo")
    Boolean ativo,

    @NotNull (message = "Deve informar a versão")
    @PositiveOrZero (message = "Versão não pode ser menor que 0")
    Long versao
) {}
