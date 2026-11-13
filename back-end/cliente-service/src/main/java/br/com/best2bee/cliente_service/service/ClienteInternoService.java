package br.com.best2bee.cliente_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.best2bee.cliente_service.dto.DestinatarioDTO;
import br.com.best2bee.cliente_service.repository.ClienteRepository;

@Service
public class ClienteInternoService {
    
    private final ClienteRepository repository;

    public ClienteInternoService(ClienteRepository repository){
        this.repository = repository;
    }

    @Transactional (readOnly = true)
    public DestinatarioDTO buscarDestinatario(Integer id){

        if (id == null || id <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deve inserir ID positivo para buscar destinatário");
        }

        var cliente = repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

        return new DestinatarioDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.isAtivo()
        );
    }
}
