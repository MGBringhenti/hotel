package br.com.best2bee.cliente_service.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.best2bee.cliente_service.dto.LoginServicoDTO;
import br.com.best2bee.cliente_service.dto.TokenDTO;
import br.com.best2bee.cliente_service.service.AuthServicoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth/servicos")
public class AuthServicoController {

    private final AuthServicoService service;

    public AuthServicoController(AuthServicoService service) {
        this.service = service;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenDTO> autenticar(
            @Valid @RequestBody LoginServicoDTO dto
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(service.autenticar(dto));
    }
}