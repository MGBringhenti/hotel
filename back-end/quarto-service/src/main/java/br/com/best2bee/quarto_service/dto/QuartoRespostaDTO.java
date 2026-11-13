package br.com.best2bee.quarto_service.dto;

import br.com.best2bee.quarto_service.enums.QuartoStatus;
import br.com.best2bee.quarto_service.model.Quarto;

public record QuartoRespostaDTO (Integer id, Integer numero, QuartoStatus status, TipoQuartoRespostaDTO tipo, Long versao) {
    
    public static QuartoRespostaDTO from (Quarto quarto){
        return new QuartoRespostaDTO
        (quarto.getId(), 
        quarto.getNumero(), 
        quarto.getStatus(), 
        TipoQuartoRespostaDTO.from(quarto.getTipo()), 
        quarto.getVersao());
    }
}
