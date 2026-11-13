package br.com.best2bee.cliente_service.controller;

import br.com.best2bee.cliente_service.dto.RedefinirSenhaDTO;
import br.com.best2bee.cliente_service.dto.SolicitarRecuperacaoSenhaDTO;
import br.com.best2bee.cliente_service.service.RecuperacaoSenhaService;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/recuperacao-senha")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public RecuperacaoSenhaController(
            RecuperacaoSenhaService recuperacaoSenhaService) {

        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> solicitar(
            @Valid @RequestBody SolicitarRecuperacaoSenhaDTO dto) {

        recuperacaoSenhaService.solicitar(dto);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "mensagem",
                        "Se houver uma conta ativa com esse e-mail, "
                                + "você receberá as instruções de recuperação."
                ));
    }

    @PostMapping("/redefinir")
    public ResponseEntity<Void> redefinir(
            @Valid @RequestBody RedefinirSenhaDTO dto) {

        recuperacaoSenhaService.redefinir(dto);

        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }
}