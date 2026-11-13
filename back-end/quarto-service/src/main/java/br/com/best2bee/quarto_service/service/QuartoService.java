package br.com.best2bee.quarto_service.service;

import org.springframework.stereotype.Service;

import br.com.best2bee.quarto_service.repository.QuartoRepository;
import br.com.best2bee.quarto_service.model.Quarto;
import br.com.best2bee.quarto_service.dto.QuartoDTO;
import br.com.best2bee.quarto_service.exception.RecursoNaoEncontradoException;
import br.com.best2bee.quarto_service.repository.TipoQuartoRepository;
import br.com.best2bee.quarto_service.dto.AtualizarQuartoDTO;
import br.com.best2bee.quarto_service.dto.AlterarStatusQuartoDTO;
import br.com.best2bee.quarto_service.enums.QuartoStatus;
import br.com.best2bee.quarto_service.dto.QuartoRespostaDTO;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class QuartoService {

    private final QuartoRepository quartoRepository;
    private final TipoQuartoRepository tipoQuartoRepository;

    public QuartoService(QuartoRepository quartoRepository, TipoQuartoRepository tipoQuartoRepository) {
        this.quartoRepository = quartoRepository;
        this.tipoQuartoRepository = tipoQuartoRepository;
    }

    @Transactional(readOnly = true)
    public Page<QuartoRespostaDTO> listarTodosQuartos(
            int pagina, int tamanho) {

        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A página deve ser maior ou igual a zero e o tamanho entre 1 e 100");
        }

        var paginacao = PageRequest.of(
                pagina,
                tamanho,
                Sort.by("id").ascending());

        return quartoRepository.findAll(paginacao)
                .map(QuartoRespostaDTO::from);
    }

    public Quarto buscarQuartoPorId(Integer id) {
        return buscarExistenciaQuarto(id);
    }

    public Quarto criarQuarto(QuartoDTO dto) {
        Quarto quarto = new Quarto();
        preencherDados(quarto, dto);
        return quartoRepository.save(quarto);
    }

    @Transactional
    public Quarto atualizarQuarto(Integer id, AtualizarQuartoDTO dto) {
        Quarto quartoExistente = buscarExistenciaQuarto(id);

        if (dto.versao() == null || !dto.versao().equals(quartoExistente.getVersao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O quarto foi alterado, confira os dados novamente");
        }
        var tipo = tipoQuartoRepository.findById(dto.idTipo())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Tipo de quarto não encontrado com o ID" + dto.idTipo()));

        quartoExistente.setNumero(dto.numero());
        quartoExistente.setTipo(tipo);

        return quartoRepository.saveAndFlush(quartoExistente);
    }

    @Transactional
    public Quarto alterarStatus(Integer id, AlterarStatusQuartoDTO dto) {
        Quarto quarto = buscarExistenciaQuarto(id);

        if (dto.versao() == null || !dto.versao().equals(quarto.getVersao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O quarto foi alterado, confira os dados novamente");
        }
        quarto.setStatus(dto.status());

        return quartoRepository.saveAndFlush(quarto);
    }

    private Quarto buscarExistenciaQuarto(Integer id) {
        return quartoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quarto não encontrado com o ID: " + id));
    }

    private void preencherDados(Quarto quarto, QuartoDTO dto) {

        var tipo = tipoQuartoRepository.findById(dto.getIdTipo())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Tipo de quarto não encontrado com o ID: " + dto.getIdTipo()));

        quarto.setNumero(dto.getNumero());
        quarto.setStatus(QuartoStatus.DISPONIVEL);
        quarto.setTipo(tipo);
    }
}
