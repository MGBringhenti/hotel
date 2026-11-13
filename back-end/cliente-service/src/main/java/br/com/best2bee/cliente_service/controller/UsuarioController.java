package br.com.best2bee.cliente_service.controller;


import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import br.com.best2bee.cliente_service.dto.AlterarSituacaoUsuarioDTO;
import br.com.best2bee.cliente_service.dto.UsuarioDTO;
import br.com.best2bee.cliente_service.model.Usuario;
import br.com.best2bee.cliente_service.service.UsuarioService;
import br.com.best2bee.cliente_service.dto.UsuarioRespostaDTO;
import br.com.best2bee.cliente_service.dto.AtualizarUsuarioDTO;

@RestController 
@RequestMapping ("/usuarios")
public class UsuarioController {
    
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioRespostaDTO>> listarTodos(@RequestParam (name = "pagina", defaultValue = "0") int pagina,
                                                                @RequestParam (name = "tamanho", defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(usuarioService.getAllUsuarios(pagina, tamanho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioRespostaDTO> getUsuarioById(
            @PathVariable Integer id) {
    
        Usuario usuario = usuarioService.getUsuarioById(id);
        return ResponseEntity.ok(UsuarioRespostaDTO.from(usuario));
    }

    @PostMapping
    public ResponseEntity<UsuarioRespostaDTO> salvar(
            @Valid @RequestBody UsuarioDTO dto) {

        Usuario usuarioSalvo = usuarioService.createUsuario(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioRespostaDTO.from(usuarioSalvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioRespostaDTO> atualizarUsuario(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarUsuarioDTO dto) {

        Usuario usuarioAtualizado = usuarioService.updateUsuario(id, dto);

        return ResponseEntity.ok(UsuarioRespostaDTO.from(usuarioAtualizado));
    }

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<UsuarioRespostaDTO> alterarSituacao(@PathVariable Integer id, @Valid @RequestBody AlterarSituacaoUsuarioDTO dto){

        return ResponseEntity.ok(usuarioService.atualizarSituacao(id, dto));
    }
    
    
}
