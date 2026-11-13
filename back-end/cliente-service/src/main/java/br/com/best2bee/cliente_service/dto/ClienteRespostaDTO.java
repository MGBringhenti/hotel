package br.com.best2bee.cliente_service.dto;

import br.com.best2bee.cliente_service.model.Cliente;

public record ClienteRespostaDTO(
        Integer id, String nome, String cpf, String email,
        String telefone, boolean ativo, Long versao) {

    public static ClienteRespostaDTO from(Cliente c) {
        return new ClienteRespostaDTO(c.getId(), c.getNome(), c.getCpf(),
                c.getEmail(), c.getTelefone(), c.isAtivo(), c.getVersao());
    }
}