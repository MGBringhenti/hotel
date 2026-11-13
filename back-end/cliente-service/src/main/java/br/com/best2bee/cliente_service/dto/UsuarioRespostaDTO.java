package br.com.best2bee.cliente_service.dto;

import br.com.best2bee.cliente_service.model.Usuario;

public record UsuarioRespostaDTO (
    Integer id,
    String login,
    String emailRecuperacao,
    String funcao,
    Boolean ativo,
    Long versao
) {
    
    public static UsuarioRespostaDTO from(Usuario usuario) {
        return new UsuarioRespostaDTO(
            usuario.getId(),
            usuario.getLogin(),
            usuario.getEmailRecuperacao(),
            usuario.getFuncao().name(),
            usuario.isAtivo(),
            usuario.getVersao()
        );
    }
}
