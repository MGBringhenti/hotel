package br.com.best2bee.reserva_service.client;

import br.com.best2bee.reserva_service.dto.QuartoConsultaDTO;
import br.com.best2bee.reserva_service.exception.RecursoNaoEncontradoException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component 
public class QuartoClient {

    private final RestClient restClient;

    public QuartoClient(@Value("${services.quarto.url}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public QuartoConsultaDTO buscarPorId(Integer id, String token) {

        if (id == null || id <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do quarto deve ser positivo ");
        }
        try {
            QuartoConsultaDTO quarto = restClient.get()
                    .uri("/quarto/{id}", id)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(QuartoConsultaDTO.class);

            if (quarto == null
                    || !id.equals(quarto.id())
                    || quarto.status() == null
                    || !java.util.Set.of("DISPONIVEL", "INATIVO").contains(quarto.status())
                    || quarto.tipo() == null
                    || quarto.tipo().preco() == null
                    || quarto.tipo().preco().signum() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "A API de quartos retornou dados inválidos"
                );
            }

            return quarto;

        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();

            if (status == 404) {
                throw new RecursoNaoEncontradoException(
                        "Quarto não encontrado com o ID: " + id);
            }

            if (status == 401 || status == 403) {
                throw new ResponseStatusException(
                        ex.getStatusCode(),
                        "Acesso à API de quartos não autorizado"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Erro ao consultar a API de quartos"
            );

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível consultar a API de quartos"
            );
        }
    }
}
    
