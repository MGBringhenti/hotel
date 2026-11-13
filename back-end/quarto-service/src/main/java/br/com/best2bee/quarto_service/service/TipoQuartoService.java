package br.com.best2bee.quarto_service.service;

import org.springframework.stereotype.Service;

import br.com.best2bee.quarto_service.model.TipoQuarto;
import br.com.best2bee.quarto_service.dto.TipoQuartoDTO;
import br.com.best2bee.quarto_service.exception.RecursoNaoEncontradoException;
import br.com.best2bee.quarto_service.repository.TipoQuartoRepository;
import br.com.best2bee.quarto_service.dto.AtualizarTipoQuartoDTO;
import br.com.best2bee.quarto_service.dto.TipoQuartoRespostaDTO;
import br.com.best2bee.quarto_service.repository.QuartoRepository;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service 
public class TipoQuartoService {
    
    private final TipoQuartoRepository tipoQuartoRepository;
    private final QuartoRepository quartoRepository;

    public TipoQuartoService(TipoQuartoRepository tipoQuartoRepository, QuartoRepository quartoRepository){
        this.tipoQuartoRepository = tipoQuartoRepository;
        this.quartoRepository = quartoRepository;
    }

    @Transactional 
    public Page<TipoQuartoRespostaDTO> listarTodosTipoQuartos(int pagina, int tamanho){
        if (pagina < 0 || tamanho < 1 || tamanho > 100){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A página não pode ser negativa, e o seu tamanho deve ser de 1 a 100");
        }
        var paginacao = PageRequest.of(pagina, tamanho, Sort.by("id").ascending());

        return tipoQuartoRepository.findAll(paginacao).map(TipoQuartoRespostaDTO::from);
    }

    public TipoQuarto getTipoQuartoById(Integer id){
        return buscarExistenciaTipoQuarto(id);
    }

    public TipoQuarto criarTipoQuarto(TipoQuartoDTO dto){
        TipoQuarto tipoQuarto = new TipoQuarto();
        preencherDados(tipoQuarto, dto);
        return tipoQuartoRepository.save(tipoQuarto);
    }

    @Transactional 
    public TipoQuarto atualizarTipoQuarto(Integer id, AtualizarTipoQuartoDTO dto){
        TipoQuarto tipoQuarto = buscarExistenciaTipoQuarto(id);

        if (dto.versao() == null || !dto.versao().equals(tipoQuarto.getVersao())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O tipo de quarto foi alterado, confira os dados novamente");
        }

        tipoQuarto.setNome(dto.nome());
        tipoQuarto.setCapacidade(dto.capacidade());
        tipoQuarto.setPreco(dto.preco());

        return tipoQuartoRepository.saveAndFlush(tipoQuarto);
    }

    public void apagarTipoQuarto(Integer id){
        buscarExistenciaTipoQuarto(id);
        
        if (quartoRepository.existsByTipo_Id(id)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível excluir um tipo de quarto em utilização");
        }
        tipoQuartoRepository.deleteById(id);
    }


    private TipoQuarto buscarExistenciaTipoQuarto(Integer id){
        return tipoQuartoRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de quarto não encontrado com o ID " + id));
    }

    private void preencherDados(TipoQuarto tipoQuarto, TipoQuartoDTO dto){
        tipoQuarto.setNome(dto.getNome());
        tipoQuarto.setCapacidade(dto.getCapacidade());
        tipoQuarto.setPreco(dto.getPreco());
    }
}
