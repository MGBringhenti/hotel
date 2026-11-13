package br.com.best2bee.hotel_mcp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HotelApiClient {

    private final RestClient quartosClient;
    private final RestClient reservasClient;
    private final String token;

    public HotelApiClient(
            @Value("${hotel.services.quarto-url}") String quartoUrl,
            @Value("${hotel.services.reserva-url}") String reservaUrl,
            @Value("${hotel.api-token}") String token) {

        this.token = token.trim();
        this.quartosClient = criarClient(quartoUrl);
        this.reservasClient = criarClient(reservaUrl);
    }

    private RestClient criarClient(String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public PaginaResposta<QuartoResposta> listarQuartos(
            int pagina, int tamanho) {

        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new IllegalArgumentException(
                    "A página deve ser maior ou igual a zero "
                            + "e o tamanho entre 1 e 100.");
        }

        return executarConsulta(() -> quartosClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quarto")
                        .queryParam("pagina", pagina)
                        .queryParam("tamanho", tamanho)
                        .build())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<PaginaResposta<QuartoResposta>>() {
                }));
    }

    public ReservaResposta consultarReserva(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "O ID da reserva deve ser maior que zero.");
        }

        return executarConsulta(() -> reservasClient.get()
                .uri("/reservas/{id}", id)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(ReservaResposta.class));
    }

    private <T> T executarConsulta(Supplier<T> consulta) {
        if (token.isBlank()) {
            throw new IllegalStateException(
                    "Configure a variável de ambiente HOTEL_API_TOKEN.");
        }

        try {
            T resposta = consulta.get();

            if (resposta == null) {
                throw new IllegalStateException(
                        "O serviço do Hotel retornou uma resposta vazia.");
            }

            return resposta;

        } catch (RestClientResponseException ex) {
            String mensagem = switch (ex.getStatusCode().value()) {
                case 401 ->
                    "Token inválido ou expirado. Renove HOTEL_API_TOKEN "
                            + "e reinicie o processo MCP.";

                case 403 ->
                    "O usuário do token não tem permissão "
                            + "para realizar esta consulta.";

                case 404 ->
                    "Registro não encontrado no Hotel.";

                default ->
                    "O serviço do Hotel retornou erro HTTP "
                            + ex.getStatusCode().value() + ".";
            };

            // Não enviar corpos de erro ou credenciais ao assistente.
            throw new IllegalStateException(mensagem);

        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Não foi possível consultar o Hotel. "
                            + "Verifique se os serviços estão funcionando "
                            + "e se as URLs estão corretas.");
        }
    }

    public record PaginaResposta<T>(
            List<T> content,
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {
    }

    public record TipoQuartoResposta(
            Integer id,
            String nome,
            Integer capacidade,
            BigDecimal preco) {
    }

    public record QuartoResposta(
            Integer id,
            Integer numero,
            String status,
            TipoQuartoResposta tipo) {
    }

    public record ReservaResposta(
            Integer idReserva,
            Integer idCliente,
            Integer idQuarto,
            LocalDate dataInicio,
            LocalDate dataFinal,
            BigDecimal valorTotal,
            String status) {
    }
}