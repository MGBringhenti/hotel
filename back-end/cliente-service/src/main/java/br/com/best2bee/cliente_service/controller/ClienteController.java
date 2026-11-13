package br.com.best2bee.cliente_service.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import br.com.best2bee.cliente_service.dto.ClienteDTO;
import br.com.best2bee.cliente_service.service.ClienteService;
import br.com.best2bee.cliente_service.dto.AlterarSituacaoClienteDTO;
import br.com.best2bee.cliente_service.dto.AtualizarClienteDTO;
import br.com.best2bee.cliente_service.dto.ClienteRespostaDTO;
import br.com.best2bee.cliente_service.dto.VincularUsuarioDTO;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<Page<ClienteRespostaDTO>> listarTodos(@RequestParam (name = "pagina", defaultValue = "0") int pagina,
                                                                @RequestParam (name = "tamanho", defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(clienteService.getAllClientes(pagina, tamanho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteRespostaDTO> getClienteById(
            @PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {

        var cliente = clienteService.consultarClienteAutorizado(id, jwt.getSubject(), jwt.getClaimAsString("funcao"));

        return ResponseEntity.ok(ClienteRespostaDTO.from(cliente));
    }

    @PostMapping
    public ResponseEntity<ClienteRespostaDTO> salvar(
            @Valid @RequestBody ClienteDTO dto) {

        var cliente = clienteService.createCliente(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteRespostaDTO.from(cliente));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteRespostaDTO> atualizarCliente(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarClienteDTO dto) {

        var cliente = clienteService.updateCliente(id, dto);

        return ResponseEntity.ok(ClienteRespostaDTO.from(cliente));
    }

    @PutMapping("/{id}/usuario")
    public ResponseEntity<ClienteRespostaDTO> vincularUsuario(
            @PathVariable Integer id,
            @Valid @RequestBody VincularUsuarioDTO dto) {

        return ResponseEntity.ok(
                clienteService.vincularUsuario(id, dto.usuarioId()));
    }

    @PatchMapping ("/{id}/situacao")
    public ResponseEntity<ClienteRespostaDTO> atualizarSituacao(@PathVariable Integer id, @Valid @RequestBody AlterarSituacaoClienteDTO dto){
        return ResponseEntity.ok(clienteService.alterarSituacao(id, dto));
    }
}
