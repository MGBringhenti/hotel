package br.com.best2bee.quarto_service.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;


import br.com.best2bee.quarto_service.service.TipoQuartoService;
import br.com.best2bee.quarto_service.dto.TipoQuartoDTO;
import br.com.best2bee.quarto_service.dto.TipoQuartoRespostaDTO;
import br.com.best2bee.quarto_service.dto.AtualizarTipoQuartoDTO;

@RestController 
@RequestMapping ("/tipoQuarto")
public class TipoQuartoController {
    
    private final TipoQuartoService tipoQuartoService;

    public TipoQuartoController(TipoQuartoService tipoQuartoService){
        this.tipoQuartoService = tipoQuartoService;
    }

    @GetMapping 
    public ResponseEntity<Page<TipoQuartoRespostaDTO>> listarTodosTiposQuartos(@RequestParam (name = "pagina", defaultValue = "0") int pagina,
                                                                               @RequestParam (name = "tamanho", defaultValue = "20") int tamanho){

        return ResponseEntity.ok(tipoQuartoService.listarTodosTipoQuartos(pagina, tamanho));
    }

    @GetMapping ("/{id}")
    public ResponseEntity<TipoQuartoRespostaDTO> listarTipoQuartoPorId(@PathVariable Integer id){
        var tipo = tipoQuartoService.getTipoQuartoById(id);
        return ResponseEntity.ok(TipoQuartoRespostaDTO.from(tipo));
    }

    @PostMapping 
    public ResponseEntity<TipoQuartoRespostaDTO> publicarTipoQuarto(@Valid @RequestBody TipoQuartoDTO dto){
        var tipo = tipoQuartoService.criarTipoQuarto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(TipoQuartoRespostaDTO.from(tipo));
    }

    @PutMapping ("/{id}")
    public ResponseEntity<TipoQuartoRespostaDTO> atualizarTipoQuarto(@PathVariable Integer id, @Valid @RequestBody AtualizarTipoQuartoDTO dto){
        var tipo = tipoQuartoService.atualizarTipoQuarto(id, dto);
        return ResponseEntity.ok(TipoQuartoRespostaDTO.from(tipo));
    }

    @DeleteMapping ("/{id}")
    public void removerQuarto(@PathVariable Integer id){
        tipoQuartoService.apagarTipoQuarto(id);
    }


}
