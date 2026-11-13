package br.com.best2bee.quarto_service.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import br.com.best2bee.quarto_service.service.QuartoService;
import br.com.best2bee.quarto_service.dto.QuartoDTO;
import br.com.best2bee.quarto_service.dto.QuartoRespostaDTO;
import br.com.best2bee.quarto_service.dto.AtualizarQuartoDTO;
import br.com.best2bee.quarto_service.dto.AlterarStatusQuartoDTO;

@RestController
@RequestMapping("/quarto")
public class QuartoController {

    private final QuartoService quartoService;

    public QuartoController(QuartoService quartoService) {
        this.quartoService = quartoService;
    }

    @GetMapping
    public ResponseEntity<Page<QuartoRespostaDTO>> listarTodosQuartos(
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "20") int tamanho) {

        return ResponseEntity.ok(
                quartoService.listarTodosQuartos(pagina, tamanho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuartoRespostaDTO> listarQuartoPorId(@PathVariable Integer id) {
        var quarto = quartoService.buscarQuartoPorId(id);
        return ResponseEntity.ok(QuartoRespostaDTO.from(quarto));
    }

    @PostMapping
    public ResponseEntity<QuartoRespostaDTO> publicarQuarto(@Valid @RequestBody QuartoDTO dto) {
        var quarto = quartoService.criarQuarto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuartoRespostaDTO.from(quarto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuartoRespostaDTO> atualizarQuarto(@PathVariable Integer id,
            @Valid @RequestBody AtualizarQuartoDTO dto) {
        var quarto = quartoService.atualizarQuarto(id, dto);

        return ResponseEntity.ok(QuartoRespostaDTO.from(quarto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<QuartoRespostaDTO> alterarStatus(@PathVariable Integer id,
            @Valid @RequestBody AlterarStatusQuartoDTO dto) {

        var quarto = quartoService.alterarStatus(id, dto);
        return ResponseEntity.ok(QuartoRespostaDTO.from(quarto));
    }
}
