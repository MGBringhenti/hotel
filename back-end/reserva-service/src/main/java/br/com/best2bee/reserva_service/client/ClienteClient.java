package br.com.best2bee.reserva_service.client;

import br.com.best2bee.reserva_service.dto.ClienteConsultaDTO;
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
public class ClienteClient {
    
    private final RestClient restClient;

    public ClienteClient (@Value("${services.cliente.url}") String url){
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
        .baseUrl(url)
        .requestFactory(factory)
        .build();
    }

    public ClienteConsultaDTO buscarPorId(Integer id, String token){
        if (id == null || id <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do cliente deve ser positivo");   
        }

        try {
            var cliente = restClient.get()
            .uri("/clientes/{id}", id)
            .headers(headers -> headers.setBearerAuth(token))
            .retrieve()
            .body(ClienteConsultaDTO.class);

        if (cliente == null || !id.equals(cliente.id()) || cliente.ativo() == null){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "API de clientes retornou dados inválidos");
        }

        return cliente;

        } catch(RestClientResponseException ex){
            int status = ex.getStatusCode().value();

            if (status == 404) {
                throw new RecursoNaoEncontradoException("Cliente não encontrado com o ID " + id);
            }

            if (status == 401 || status == 403){
                throw new ResponseStatusException(ex.getStatusCode(), "Acesso não autorizado à API de clientes");
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erro ao consultar a API de clientes");

        } catch(RestClientException ex){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível consultar a API de clientes");
        }
    }
}
