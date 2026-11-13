package br.com.best2bee.reserva_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import br.com.best2bee.reserva_service.exception.RecursoNaoEncontradoException;
import br.com.best2bee.reserva_service.model.Reserva;
import br.com.best2bee.reserva_service.repository.ReservaRepository;
import br.com.best2bee.reserva_service.dto.QuartoConsultaDTO;
import br.com.best2bee.reserva_service.client.QuartoClient;
import br.com.best2bee.reserva_service.client.ClienteClient;
import br.com.best2bee.reserva_service.dto.CriarReservaDTO;
import br.com.best2bee.reserva_service.dto.AlterarReservaDTO;
import br.com.best2bee.reserva_service.enums.Status;
import br.com.best2bee.reserva_service.dto.AlterarStatusDTO;
import br.com.best2bee.reserva_service.dto.CriarMinhaReservaDTO;
import br.com.best2bee.reserva_service.enums.TipoEventoReserva;
import br.com.best2bee.reserva_service.dto.DisponibilidadeQuartoDTO;
import br.com.best2bee.reserva_service.security.Ator;
import br.com.best2bee.reserva_service.dto.CalendarioQuartoDTO;
import br.com.best2bee.reserva_service.dto.PeriodoOcupadoDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.time.Clock;
import java.math.BigDecimal;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final QuartoClient quartoClient;
    private final ClienteClient clienteClient;
    private final Clock clock;
    private final OutboxService outboxService;

    public ReservaService(ReservaRepository reservaRepository, QuartoClient quartoClient, ClienteClient clienteClient,
            Clock clock, OutboxService outboxService) {

        this.reservaRepository = reservaRepository;
        this.quartoClient = quartoClient;
        this.clienteClient = clienteClient;
        this.clock = clock;
        this.outboxService = outboxService;
    }

    @Transactional(readOnly = true)
    public Page<Reserva> getAllReservas(Ator ator, Pageable pageable) {

        if (ator.equipe()) {
            return reservaRepository.findAll(pageable);
        }

        conferirIdentidadeCliente(ator);

        return reservaRepository.findByIdCliente(ator.clienteId(), pageable);
    }

    @Transactional(readOnly = true)
    public Reserva getReservaById(Integer id, Ator ator) {
        Reserva reserva = buscarExistencia(id);
        conferirAcesso(reserva, ator);
        return reserva;
    }

    @Transactional(readOnly = true)
    public DisponibilidadeQuartoDTO consultarDisponibilidade(Integer idQuarto,
            LocalDate dataInicio,
            LocalDate dataFinal,
            String token) {
        validarPeriodo(dataInicio, dataFinal);

        var quarto = quartoClient.buscarPorId(idQuarto, token);

        boolean disponivel = false;

        if ("DISPONIVEL".equals(quarto.status())) {
            long conflitos = reservaRepository.contarConflitos(idQuarto, dataInicio, dataFinal,
                    List.of(Status.PENDENTE, Status.CONFIRMADA, Status.CONCLUIDA), null);
            disponivel = conflitos == 0;
        }

        return new DisponibilidadeQuartoDTO(idQuarto, dataInicio, dataFinal, disponivel);
    }

    @Transactional(readOnly = true)
    public CalendarioQuartoDTO consultarCalendario(
            Integer idQuarto,
            LocalDate dataInicio,
            LocalDate dataFinal,
            String token) {
        long dias = validarPeriodo(dataInicio, dataFinal);

        if (dias > 366) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Consulte no máximo 366 dias por vez");
        }

        var quarto = quartoClient.buscarPorId(idQuarto, token);

        var periodos = reservaRepository.buscarPeriodosOcupados(
                idQuarto,
                dataInicio,
                dataFinal,
                List.of(
                        Status.PENDENTE,
                        Status.CONFIRMADA,
                        Status.CONCLUIDA));

        var periodosNoIntervalo = periodos.stream()
                .map(periodo -> new PeriodoOcupadoDTO(
                        periodo.dataInicio().isBefore(dataInicio)
                                ? dataInicio
                                : periodo.dataInicio(),

                        periodo.dataFinal().isAfter(dataFinal)
                                ? dataFinal
                                : periodo.dataFinal()))
                .toList();

        return new CalendarioQuartoDTO(
                idQuarto,
                "DISPONIVEL".equals(quarto.status()),
                dataInicio,
                dataFinal,
                periodosNoIntervalo);
    }

    @Transactional
    public Reserva postReserva(CriarReservaDTO dto, String token) {

        Long dias = validarPeriodo(dto.dataInicio(), dto.dataFinal());

        var quarto = validarClienteEQuarto(dto.idCliente(), dto.idQuarto(), token);

        validarDisponibilidade(dto.idQuarto(), dto.dataInicio(), dto.dataFinal(), null);

        BigDecimal diaria = validarValorDiarioRecebido(quarto.tipo().preco());

        Reserva reserva = new Reserva();
        reserva.setIdCliente(dto.idCliente());
        reserva.setIdQuarto(dto.idQuarto());
        reserva.setDataInicio(dto.dataInicio());
        reserva.setDataFinal(dto.dataFinal());
        reserva.setPrecoDiarioAplicado(diaria);
        reserva.setValorTotal(calcularValorTotal(diaria, dias));
        reserva.setStatus(Status.PENDENTE);

        Reserva salva = reservaRepository.saveAndFlush(reserva);

        outboxService.registrar(salva, TipoEventoReserva.RESERVA_CRIADA);

        return salva;

    }

    @Transactional
    public Reserva criarMinhaReserva(CriarMinhaReservaDTO dto, Ator ator, String token) {

        conferirIdentidadeCliente(ator);

        var dados = new CriarReservaDTO(ator.clienteId(), dto.idQuarto(), dto.dataInicio(), dto.dataFinal());

        return postReserva(dados, token);
    }

    @Transactional
    public Reserva putReserva(Integer id, AlterarReservaDTO dto, String token) {

        Reserva reserva = buscarExistencia(id);
        conferirVersao(reserva, dto.versao());

        Long versaoAnterior = reserva.getVersao();

        if (reserva.getStatus() != Status.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente reservas pendentes podem ter os dados alterados");
        }

        long dias = validarPeriodo(dto.dataInicio(), dto.dataFinal());
        var quarto = validarClienteEQuarto(dto.idCliente(), dto.idQuarto(), token);

        validarDisponibilidade(dto.idQuarto(), dto.dataInicio(), dto.dataFinal(), id);

        boolean mudouQuarto = !Objects.equals(reserva.getIdQuarto(), dto.idQuarto());

        BigDecimal diaria;

        if (mudouQuarto) {
            diaria = validarValorDiarioRecebido(quarto.tipo().preco());
        } else {
            diaria = reserva.getPrecoDiarioAplicado();

            if (diaria == null || diaria.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Valor inserido de diária é inválido ");
            }
        }

        BigDecimal total = calcularValorTotal(diaria, dias);

        reserva.setIdCliente(dto.idCliente());
        reserva.setIdQuarto(dto.idQuarto());
        reserva.setDataInicio(dto.dataInicio());
        reserva.setDataFinal(dto.dataFinal());
        reserva.setPrecoDiarioAplicado(diaria);
        reserva.setValorTotal(total);

        Reserva salva = reservaRepository.saveAndFlush(reserva);

        if (!Objects.equals(versaoAnterior, salva.getVersao())) {
            outboxService.registrar(salva, TipoEventoReserva.RESERVA_ALTERADA);
        }
        return salva;
    }

    @Transactional
    public Reserva alterarStatus(Integer id, AlterarStatusDTO dto, Ator ator, String token) {

        Reserva reserva = buscarExistencia(id);

        conferirAcesso(reserva, ator);
        conferirVersao(reserva, dto.versao());

        Status atual = reserva.getStatus();
        Status novo = dto.status();

        if (novo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status novo é obrigatório");
        }

        if (atual == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status atual da reserva deve ser regularizado");
        }
        if (!ator.equipe() && novo != Status.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cliente só pode cancelar a própria reserva");
        }

        boolean transicaoPermitida;

        switch (atual) {

            case PENDENTE:
                transicaoPermitida = novo == Status.CONFIRMADA || novo == Status.CANCELADA;
                break;
            case CONFIRMADA:
                transicaoPermitida = novo == Status.CONCLUIDA || novo == Status.CANCELADA;
                break;
            default:
                transicaoPermitida = false;
        }
        if (!transicaoPermitida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa alteração de status não pode ser realizada");
        }

        LocalDate hoje = LocalDate.now(clock);

        if (novo == Status.CANCELADA && !hoje.isBefore(reserva.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não pode cancelar a reserva após iniciá-la");
        }

        if (novo == Status.CONFIRMADA && hoje.isAfter(reserva.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não pode confirmar uma reserva após sua data inicial");
        }

        if (novo == Status.CONCLUIDA && hoje.isBefore(reserva.getDataFinal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não pode concluir uma reserva antes de sua data final");
        }

        if (novo == Status.CONFIRMADA) {
            validarClienteEQuarto(reserva.getIdCliente(), reserva.getIdQuarto(), token);
        }

        reserva.setStatus(novo);

        Reserva salva = reservaRepository.saveAndFlush(reserva);

        outboxService.registrar(salva, TipoEventoReserva.RESERVA_STATUS_ALTERADO);

        return salva;
    }

    private Reserva buscarExistencia(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada com o ID " + id));
    }

    private Long validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "As datas de entrada e saída são obrigatórias ");
        }

        LocalDate hoje = LocalDate.now(clock);

        if (inicio.isBefore(hoje)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de entrada não pode estar no passado");
        }

        if (!fim.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data de saída deve ser posterior à data de entrada");
        }

        return ChronoUnit.DAYS.between(inicio, fim);
    }

    private BigDecimal validarValorDiarioRecebido(BigDecimal diaria) {
        if (diaria == null || diaria.signum() <= 0 || diaria.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A API de quartos retornou uma diária inválida");
        }

        try {
            return diaria.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A diária deve ter até duas casas decimais");
        }
    }

    private BigDecimal calcularValorTotal(BigDecimal diaria, Long dias) {
        BigDecimal total = diaria.multiply(BigDecimal.valueOf(dias));

        if (total.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O valor total excede o limite permitido");
        }

        return total.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void conferirVersao(Reserva reserva, Long versaoInformada) {
        if (versaoInformada == null || reserva.getVersao() == null || !versaoInformada.equals(reserva.getVersao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A reserva foi alterada, confira os dados novamente");
        }
    }

    private void conferirIdentidadeCliente(Ator ator) {
        if (!"CLIENTE".equals(ator.funcao()) || ator.clienteId() == null || ator.clienteId() <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidade de cliente inválida");
        }
    }

    private void conferirAcesso(Reserva reserva, Ator ator) {

        if (ator.equipe()) {
            return;
        }
        conferirIdentidadeCliente(ator);

        if (!ator.clienteId().equals(reserva.getIdCliente())) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada");
        }
    }

    private QuartoConsultaDTO validarClienteEQuarto(Integer idCliente, Integer idQuarto, String token) {
        var cliente = clienteClient.buscarPorId(idCliente, token);

        if (!Boolean.TRUE.equals(cliente.ativo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível marcar reserva sem ser cliente ou estando inativo ");
        }

        var quarto = quartoClient.buscarPorId(idQuarto, token);

        if (!"DISPONIVEL".equals(quarto.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível marcar reserva em um quarto inativo");
        }
        return quarto;
    }

    private void validarDisponibilidade(Integer quartoId, LocalDate inicio, LocalDate fim, Integer ignorarId) {
        long conflitos = reservaRepository.contarConflitos(quartoId, inicio, fim, List.of(
                Status.PENDENTE,
                Status.CONFIRMADA,
                Status.CONCLUIDA), ignorarId);

        if (conflitos > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O quarto já possui uma reserva nesse período ");
        }

    }
}
