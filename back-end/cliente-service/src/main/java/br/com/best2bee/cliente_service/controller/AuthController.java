package br.com.best2bee.cliente_service.controller;

import br.com.best2bee.cliente_service.dto.LoginDTO;
import br.com.best2bee.cliente_service.dto.TokenDTO;
import br.com.best2bee.cliente_service.service.AuthService;
import br.com.best2bee.cliente_service.dto.AlterarSenhaDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PatchMapping ("/senha")
    public ResponseEntity<Void> alterarSenha (@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AlterarSenhaDTO dto){
        authService.alterarSenha(jwt.getSubject(), dto);
        return ResponseEntity.noContent().build();
    }
}
