package br.com.best2bee.quarto_service.dto;

import java.math.BigDecimal;

import br.com.best2bee.quarto_service.model.TipoQuarto;

public record TipoQuartoRespostaDTO 
(Integer id, String nome, Integer capacidade, BigDecimal preco, Long versao) {

    public static TipoQuartoRespostaDTO from(TipoQuarto tipo){
        return new TipoQuartoRespostaDTO
        (tipo.getId(), 
         tipo.getNome(), 
         tipo.getCapacidade(), 
         tipo.getPreco(), 
         tipo.getVersao());
    }
}
