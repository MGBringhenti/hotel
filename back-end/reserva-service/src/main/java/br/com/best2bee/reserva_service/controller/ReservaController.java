package br.com.best2bee.reserva_service.controller;

import br.com.best2bee.reserva_service.service.ReservaService;
import br.com.best2bee.reserva_service.dto.CriarReservaDTO;
import br.com.best2bee.reserva_service.dto.AlterarReservaDTO;
import br.com.best2bee.reserva_service.dto.ReservaRespostaDTO;
import br.com.best2bee.reserva_service.security.Ator;
import br.com.best2bee.reserva_service.dto.AlterarStatusDTO;
import br.com.best2bee.reserva_service.dto.CriarMinhaReservaDTO;
import br.com.best2bee.reserva_service.dto.DisponibilidadeQuartoDTO;
import br.com.best2bee.reserva_service.dto.CalendarioQuartoDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ResponseEntity<Page<ReservaRespostaDTO>> buscarTodasReservas(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {

        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A página deve ser >= 0 e o tamanho entre 1 e 100");
        }

        var pageable = PageRequest.of(pagina, tamanho, Sort.by("idReserva").ascending());

        var reservas = reservaService.getAllReservas(Ator.from(jwt), pageable);

        return ResponseEntity.ok(reservas.map(ReservaRespostaDTO::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaRespostaDTO> buscarReservaPorId(@PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt) {
        var reserva = reservaService.getReservaById(id, Ator.from(jwt));
        return ResponseEntity.ok(ReservaRespostaDTO.from(reserva));
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<DisponibilidadeQuartoDTO> consultarDisponibilidade(
            @RequestParam Integer idQuarto,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,

            @AuthenticationPrincipal Jwt jwt) {
        var resposta = reservaService.consultarDisponibilidade(
                idQuarto,
                dataInicio,
                dataFinal,
                jwt.getTokenValue());

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/calendario")
    public ResponseEntity<CalendarioQuartoDTO> consultarCalendario(
            @RequestParam Integer idQuarto,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,

            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                reservaService.consultarCalendario(
                        idQuarto,
                        dataInicio,
                        dataFinal,
                        jwt.getTokenValue()));
    }

    @PostMapping
    public ResponseEntity<ReservaRespostaDTO> fazerReserva(
            @Valid @RequestBody CriarReservaDTO dto, @AuthenticationPrincipal Jwt jwt) {

        var reserva = reservaService.postReserva(dto, jwt.getTokenValue());

        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaRespostaDTO.from(reserva));
    }

    @PostMapping("/minhas")
    public ResponseEntity<ReservaRespostaDTO> fazerMinhaReserva(@Valid @RequestBody CriarMinhaReservaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {

        var reserva = reservaService.criarMinhaReserva(dto, Ator.from(jwt), jwt.getTokenValue());

        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaRespostaDTO.from(reserva));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaRespostaDTO> editarReserva(
            @PathVariable Integer id,
            @Valid @RequestBody AlterarReservaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {

        var reserva = reservaService.putReserva(
                id, dto, jwt.getTokenValue());

        return ResponseEntity.ok(ReservaRespostaDTO.from(reserva));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservaRespostaDTO> alterarStatus(@PathVariable Integer id,
            @Valid @RequestBody AlterarStatusDTO dto, @AuthenticationPrincipal Jwt jwt) {
        var reserva = reservaService.alterarStatus(id, dto, Ator.from(jwt), jwt.getTokenValue());

        return ResponseEntity.ok(ReservaRespostaDTO.from(reserva));
    }
}
