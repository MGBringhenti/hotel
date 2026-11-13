package br.com.best2bee.notificacoes_service.service;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.best2bee.notificacoes_service.client.ClienteInternoClient;
import br.com.best2bee.notificacoes_service.dto.ReservaEventoDTO;
import br.com.best2bee.notificacoes_service.model.Notificacao;
import br.com.best2bee.notificacoes_service.repository.EventoRecebidoRepository;
import br.com.best2bee.notificacoes_service.repository.NotificacaoRepository;
import tools.jackson.databind.json.JsonMapper;

@Service
public class NotificacaoProcessador {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoProcessador.class);

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int MAX_TENTATIVAS = 10;

    private final NotificacaoRepository notificacaoRepository;
    private final EventoRecebidoRepository eventoRepository;
    private final ClienteInternoClient clienteClient;
    private final EmailService emailService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public NotificacaoProcessador(
            NotificacaoRepository notificacaoRepository,
            EventoRecebidoRepository eventoRepository,
            ClienteInternoClient clienteClient,
            EmailService emailService,
            JsonMapper jsonMapper,
            Clock clock) {
        this.notificacaoRepository = notificacaoRepository;
        this.eventoRepository = eventoRepository;
        this.clienteClient = clienteClient;
        this.emailService = emailService;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Transactional
    public boolean processarProximo() {
        var pendente = notificacaoRepository.buscarProximaParaEnviar(clock.instant());

        if (pendente.isEmpty()) {
            return false;
        }

        var notificacao = pendente.get();

        var evento = eventoRepository.findById(notificacao.getEventoId())
                .orElseThrow(() -> new IllegalStateException("Evento da notificação não encontrado"));

        boolean existeMaisRecente = eventoRepository.existsByReservaIdAndReservaVersaoGreaterThan(
                evento.getReservaId(), evento.getReservaVersao());

        if (existeMaisRecente) {
            descartar(notificacao, "EVENTO_SUPERADO");
            return true;
        }

        int tentativas = notificacao.getTentativas() == Integer.MAX_VALUE ? Integer.MAX_VALUE
                : notificacao.getTentativas() + 1;

        notificacao.setTentativas(tentativas);

        try {

            var dados = jsonMapper.readValue(evento.getPayload(), ReservaEventoDTO.class);
            var destinatario = clienteClient.buscarDestinatario(notificacao.getClienteId());

            String assunto = "Atualização da reserva #" + dados.idReserva();
            String texto = montarTexto(destinatario.nome(), dados);
            emailService.enviar(destinatario.email(), assunto, texto);

        } catch (RuntimeException ex) {

            if (Thread.currentThread().isInterrupted()) {
                throw ex;
            }

            if (tentativas >= MAX_TENTATIVAS) {
                descartar(notificacao, "LIMITE_TENTATIVAS");

                log.error(
                        "Envio da notificação {} interrompido após {} "
                                + "tentativas. Tipo da falha: {}",
                        notificacao.getEventoId(),
                        tentativas,
                        ex.getClass().getSimpleName());

                return true;
            }

            long atrasoSegundos = Math.min(
                    300L,
                    5L * (1L << Math.min(tentativas - 1, 6)));

            notificacao.setProximaTentativaEm(
                    clock.instant().plusSeconds(atrasoSegundos));

            log.warn(
                    "Falha na notificação {}. Tentativa {}. "
                            + "Nova tentativa em {} segundos. Tipo da falha: {}",
                    notificacao.getEventoId(),
                    tentativas,
                    atrasoSegundos,
                    ex.getClass().getSimpleName());

            return true;
        }
        
        notificacao.setEnviadoEm(clock.instant());
        return true;
    }

    private void descartar(Notificacao notificacao, String motivo) {
        notificacao.setDescartadoEm(clock.instant());
        notificacao.setMotivoDescarte(motivo);

        log.info(
                "Notificação {} descartada: {}",
                notificacao.getEventoId(),
                motivo);
    }

    private String montarTexto(String nome, ReservaEventoDTO dados) {
        String status = switch (dados.status()) {
            case "PENDENTE" -> "Pendente";
            case "CONFIRMADA" -> "Confirmada";
            case "CANCELADA" -> "Cancelada";
            case "CONCLUIDA" -> "Concluída";
            default -> throw new IllegalArgumentException(
                    "Status de reserva não suportado");
        };

        String valor = String.format(
                Locale.forLanguageTag("pt-BR"),
                "%.2f",
                dados.valorTotal());

        return """
                Olá, %s!

                Registramos uma atualização da sua reserva.

                Reserva: #%s
                Situação registrada: %s
                Entrada: %s
                Saída: %s
                Valor total: R$ %s

                Consulte o sistema para acompanhar a situação atual.

                Equipe Hotel
                """.formatted(
                nome,
                dados.idReserva(),
                status,
                dados.dataInicio().format(DATA),
                dados.dataFinal().format(DATA),
                valor);
    }
}
