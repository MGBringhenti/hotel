package br.com.best2bee.reserva_service.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public record Ator(
    String usuarioId,
    String funcao,
    Integer clienteId
) {
    public static Ator from(Jwt jwt) {
        String funcao = jwt.getClaimAsString("funcao");

        boolean equipe = "ADMIN".equals(funcao) || "RECEPCIONISTA".equals(funcao);

        if (!equipe && !"CLIENTE".equals(funcao)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Função não autorizada");
        }

        Integer clienteId = null;

        if ("CLIENTE".equals(funcao)){
            Object claim = jwt.getClaims().get("clienteId");

            try {
                clienteId = Integer.valueOf(String.valueOf(claim));

                if (clienteId <= 0){
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex){
                throw new ResponseStatusException
                (HttpStatus.FORBIDDEN, "Token sem vínculo de cliente válido");
            }
        }

        return new Ator(jwt.getSubject(), funcao, clienteId);
    }

    public boolean equipe() {
        return "ADMIN".equals(funcao) || "RECEPCIONISTA".equals(funcao);
    }
}
