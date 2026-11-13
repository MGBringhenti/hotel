package br.com.best2bee.notificacoes_service.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import br.com.best2bee.notificacoes_service.dto.DestinatarioDTO;

@Component
public class ClienteInternoClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    private String tokenAtual;
    private Instant renovarEm = Instant.EPOCH;

    public ClienteInternoClient(
            RestClient.Builder builder,
            @Value("${hotel.cliente-service.url}") String url,
            @Value("${hotel.cliente-service.client-id}") String clientId,
            @Value("${hotel.cliente-service.client-secret}") String clientSecret
    ) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = builder
                .baseUrl(url)
                .requestFactory(requestFactory)
                .build();

        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public DestinatarioDTO buscarDestinatario(Integer clienteId) {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException(
                    "Identificador do cliente inválido"
            );
        }

        String token = obterToken();

        try {
            return consultar(clienteId, token);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() != 401) {
                throw ex;
            }

            invalidarToken(token);

            // Renova e repete a consulta apenas uma vez.
            return consultar(clienteId, obterToken());
        }
    }

    private DestinatarioDTO consultar(Integer clienteId, String token) {
        var destinatario = restClient.get()
                .uri("/interno/clientes/{id}/destinatario", clienteId)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(DestinatarioDTO.class);

        if (destinatario == null
                || !clienteId.equals(destinatario.id())
                || destinatario.nome() == null
                || destinatario.nome().isBlank()
                || destinatario.email() == null
                || destinatario.email().isBlank()
                || destinatario.ativo() == null) {
            throw new IllegalStateException(
                    "Cliente-service retornou um destinatário incompleto ou inválido"
            );
        }

        return destinatario;
    }

    private synchronized String obterToken() {
        Instant agora = Instant.now();

        if (tokenAtual != null && agora.isBefore(renovarEm)) {
            return tokenAtual;
        }

        var resposta = restClient.post()
                .uri("/auth/servicos/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "clientId", clientId,
                        "clientSecret", clientSecret
                ))
                .retrieve()
                .body(TokenRespostaDTO.class);

        if (resposta == null
                || resposta.accessToken() == null
                || resposta.accessToken().isBlank()
                || !"Bearer".equalsIgnoreCase(resposta.tokenType())
                || resposta.expiresIn() == null
                || resposta.expiresIn() <= 30
                || resposta.expiresIn() > 3600) {
            throw new IllegalStateException(
                    "Cliente-service retornou uma resposta de autenticação inválida"
            );
        }

        // Usa o início da requisição como referência e reserva 30 segundos.
        Instant novaRenovacao = agora.plusSeconds(
                resposta.expiresIn() - 30
        );

        if (!Instant.now().isBefore(novaRenovacao)) {
            throw new IllegalStateException(
                    "Token recebido sem tempo útil de validade"
            );
        }

        tokenAtual = resposta.accessToken();
        renovarEm = novaRenovacao;

        return tokenAtual;
    }

    private synchronized void invalidarToken(String tokenRejeitado) {
        if (Objects.equals(tokenAtual, tokenRejeitado)) {
            tokenAtual = null;
            renovarEm = Instant.EPOCH;
        }
    }

    public record TokenRespostaDTO(
            String accessToken,
            String tokenType,
            Long expiresIn
    ) {
    }
}