package br.com.best2bee.cliente_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import br.com.best2bee.cliente_service.model.Cliente;
import br.com.best2bee.cliente_service.model.Usuario;
import br.com.best2bee.cliente_service.repository.ClienteRepository;
import br.com.best2bee.cliente_service.repository.UsuarioRepository;
import br.com.best2bee.cliente_service.dto.ClienteDTO;
import br.com.best2bee.cliente_service.dto.ClienteRespostaDTO;
import br.com.best2bee.cliente_service.enums.Funcao;
import br.com.best2bee.cliente_service.exception.RecursoNaoEncontradoException;
import br.com.best2bee.cliente_service.dto.AlterarSituacaoClienteDTO;
import br.com.best2bee.cliente_service.dto.AtualizarClienteDTO;

import java.util.Locale;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public ClienteService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Page<ClienteRespostaDTO> getAllClientes(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A página deve ser maior ou igual a zero, e ter tamanho entre 1 e 100");
        }

        var paginacao = PageRequest.of(pagina, tamanho, Sort.by("id").ascending());
        return clienteRepository.findAll(paginacao).map(ClienteRespostaDTO::from);
    }

    @Transactional
    public ClienteRespostaDTO vincularUsuario(Integer clienteId, Integer usuarioId) {
        Cliente cliente = buscarClienteOuLancarExcecao(clienteId);

        if (!cliente.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível vincular usuário inativo");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário inexistente"));

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível vincular uma conta à um cliente inativo");
        }

        if (usuario.getFuncao() != Funcao.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário deve ser cliente");
        }

        if (cliente.getUsuario() != null) {
            if (cliente.getUsuario().getId().equals(usuarioId)) {
                return ClienteRespostaDTO.from(cliente);
            }

            throw new ResponseStatusException(HttpStatus.CONFLICT, "O cliente já possui uma conta vinculada");
        }

        if (clienteRepository.existsByUsuario_IdAndIdNot(usuarioId, clienteId)) {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "A conta já está vinculada à outro cliente");
        }

        cliente.setUsuario(usuario);

        return ClienteRespostaDTO.from(clienteRepository.saveAndFlush(cliente));
    }

    public Cliente getClienteById(Integer id) {
        return buscarClienteOuLancarExcecao(id);
    }

    @Transactional(readOnly = true)
    public Cliente consultarClienteAutorizado(
            Integer id, String usuarioId, String funcao) {

        if ("ADMIN".equals(funcao) || "RECEPCIONISTA".equals(funcao)) {
            return buscarClienteOuLancarExcecao(id);
        }

        if (!"CLIENTE".equals(funcao)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Função não autorizada");
        }

        Integer idUsuario;

        try {
            idUsuario = Integer.valueOf(usuarioId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Identidade de usuário inválida");
        }

        var cliente = clienteRepository.findByUsuario_Id(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado"));

        if (!cliente.getId().equals(id)) {
            throw new RecursoNaoEncontradoException(
                    "Cliente não encontrado");
        }

        if (!cliente.getUsuario().isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Conta inativa");
        }

        return cliente;
    }

    public Cliente createCliente(ClienteDTO dto) {
        Cliente cliente = new Cliente();
        preencherDados(cliente, dto);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente updateCliente(Integer id, AtualizarClienteDTO dto) {
        Cliente cliente = buscarClienteOuLancarExcecao(id);

        if (dto.versao() == null
                || !dto.versao().equals(cliente.getVersao())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O cliente foi alterado. Consulte os dados novamente.");
        }

        cliente.setNome(dto.nome());
        cliente.setCpf(normalizarCPF(dto.cpf()));
        cliente.setEmail(normalizarEmail(dto.email()));
        cliente.setTelefone(dto.telefone());

        return clienteRepository.saveAndFlush(cliente);
    }

    @Transactional
    public ClienteRespostaDTO alterarSituacao(Integer id, AlterarSituacaoClienteDTO dto) {

        Cliente cliente = buscarClienteOuLancarExcecao(id);

        if (dto.versao() == null || cliente.getVersao() == null || !dto.versao().equals(cliente.getVersao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O cliente foi atualizado, confira os dados novamente");
        }

        cliente.setAtivo(dto.ativo());
        return ClienteRespostaDTO.from(clienteRepository.saveAndFlush(cliente));
    }

    private void preencherDados(Cliente cliente, ClienteDTO dto) {
        cliente.setNome(dto.getNome());
        cliente.setCpf(normalizarCPF(dto.getCpf()));
        cliente.setEmail(normalizarEmail(dto.getEmail()));
        cliente.setTelefone(dto.getTelefone());
    }

    private Cliente buscarClienteOuLancarExcecao(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + id));
    }

    private String normalizarCPF(String cpf) {
        return cpf.replace(".", "").replace("-", "");
    }

    private String normalizarEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
