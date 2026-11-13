package br.com.best2bee.cliente_service.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.best2bee.cliente_service.dto.DestinatarioDTO;
import br.com.best2bee.cliente_service.service.ClienteInternoService;

@RestController
@RequestMapping("/interno/clientes")
public class ClienteInternoController {

    private final ClienteInternoService service;

    public ClienteInternoController(ClienteInternoService service) {
        this.service = service;
    }

    @GetMapping("/{id}/destinatario")
    public ResponseEntity<DestinatarioDTO> buscarDestinatario(
            @PathVariable("id") Integer id
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.buscarDestinatario(id));
    }
}
