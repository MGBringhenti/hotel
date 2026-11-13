package br.com.best2bee.notificacoes_service.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.best2bee.notificacoes_service.dto.EventoKafkaDTO;
import br.com.best2bee.notificacoes_service.repository.EventoRecebidoRepository;
import br.com.best2bee.notificacoes_service.repository.NotificacaoRepository;

import tools.jackson.databind.json.JsonMapper;

@Service
public class EventoRecebidoService {
    
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
        "RESERVA_CRIADA",
        "RESERVA_ALTERADA",
        "RESERVA_STATUS_ALTERADO"
    );

    private static final Set<String> STATUS_PERMITIDOS = Set.of(
        "PENDENTE",
        "CONFIRMADA",
        "CANCELADA",
        "CONCLUIDA"
    );

    private final EventoRecebidoRepository repository;
    private final JsonMapper jsonMapper;
    private final NotificacaoRepository notificacaoRepository;

    public EventoRecebidoService(EventoRecebidoRepository repository, JsonMapper jsonMapper, NotificacaoRepository notificacaoRepository){
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.notificacaoRepository = notificacaoRepository;
    }

     @Transactional
    public boolean registrar(EventoKafkaDTO evento) {
        validar(evento);

        var dados = evento.dados();

        int inseridos = repository.inserirSeAusente(
                evento.eventoId(),
                evento.tipo(),
                dados.idReserva(),
                dados.versaoReserva(),
                jsonMapper.writeValueAsString(dados),
                evento.criadoEm()
        );

        if (inseridos == 0){
                return false;
        }

        notificacaoRepository.inserir(evento.eventoId(), evento.dados().idCliente());

        return true;
    }

    private void validar(EventoKafkaDTO evento) {
        exigir(evento != null, "Evento obrigatório");
        exigir(evento.eventoId() != null, "Identificador obrigatório");
        exigir(evento.criadoEm() != null, "Data do evento obrigatória");

        exigir(
                evento.tipo() != null
                        && TIPOS_PERMITIDOS.contains(evento.tipo()),
                "Tipo de evento não suportado"
        );

        var dados = evento.dados();
        exigir(dados != null, "Dados da reserva obrigatórios");

        exigir(
                Integer.valueOf(1).equals(dados.versaoFormato()),
                "Versão do formato não suportada"
        );

        exigir(
                dados.idReserva() != null && dados.idReserva() > 0,
                "Identificador da reserva inválido"
        );

        exigir(
                dados.versaoReserva() != null && dados.versaoReserva() >= 0,
                "Versão da reserva inválida"
        );

        exigir(
                dados.idCliente() != null && dados.idCliente() > 0,
                "Identificador do cliente inválido"
        );

        exigir(
                dados.idQuarto() != null && dados.idQuarto() > 0,
                "Identificador do quarto inválido"
        );

        exigir(
                dados.dataInicio() != null
                        && dados.dataFinal() != null
                        && dados.dataFinal().isAfter(dados.dataInicio()),
                "Período da reserva inválido"
        );

        exigir(
                dados.precoDiarioAplicado() != null
                        && dados.precoDiarioAplicado().signum() > 0,
                "Diária inválida"
        );

        exigir(
                dados.valorTotal() != null
                        && dados.valorTotal().signum() > 0,
                "Valor total inválido"
        );

        exigir(
                dados.status() != null
                        && STATUS_PERMITIDOS.contains(dados.status()),
                "Status da reserva inválido"
        );
    }

    private void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}