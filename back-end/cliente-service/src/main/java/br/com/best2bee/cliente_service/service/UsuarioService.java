package br.com.best2bee.cliente_service.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import br.com.best2bee.cliente_service.model.Usuario;
import br.com.best2bee.cliente_service.repository.UsuarioRepository;
import br.com.best2bee.cliente_service.dto.UsuarioDTO;
import br.com.best2bee.cliente_service.dto.UsuarioRespostaDTO;
import br.com.best2bee.cliente_service.exception.RecursoNaoEncontradoException;
import br.com.best2bee.cliente_service.dto.AlterarSituacaoUsuarioDTO;
import br.com.best2bee.cliente_service.enums.Funcao;
import br.com.best2bee.cliente_service.dto.AtualizarUsuarioDTO;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service 
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioRespostaDTO> getAllUsuarios(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A página deve ser maior que zero e seu tamanho entre 1 e 100");
        }
        var paginacao = PageRequest.of(pagina, tamanho, Sort.by("id").ascending());
        return usuarioRepository.findAll(paginacao).map(UsuarioRespostaDTO::from);
    }

    public Usuario getUsuarioById(Integer id) {
        return buscarUsuarioOuLancarExcecao(id);
    }

    public Usuario createUsuario(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        preencherDados(usuario, dto);
        return usuarioRepository.save(usuario);
    }

    @Transactional 
    public Usuario updateUsuario(Integer id, AtualizarUsuarioDTO dto) {
        Usuario usuario = buscarUsuarioOuLancarExcecao(id);

        if (dto.versao() == null || !dto.versao().equals(usuario.getVersao())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O usuário foi alterado, confira as informações novamente");
        }

        usuario.setLogin(dto.login());
        usuario.setEmailRecuperacao(dto.emailRecuperacao().strip().toLowerCase(Locale.ROOT));

        return usuarioRepository.saveAndFlush(usuario);
    }

    @Transactional 
    public UsuarioRespostaDTO atualizarSituacao(Integer id, AlterarSituacaoUsuarioDTO dto){

        Usuario usuario = buscarUsuarioOuLancarExcecao(id);

        if (dto.versao() == null || usuario.getVersao() == null || !dto.versao().equals(usuario.getVersao())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O usuário foi alterado, confira os dados novamente");
        }

        if (usuario.getFuncao() == Funcao.ADMIN && Boolean.FALSE.equals(dto.ativo())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Desativação de administradores não é permitida nessa operação");
        }

        usuario.setAtivo(dto.ativo());

        return UsuarioRespostaDTO.from(usuarioRepository.saveAndFlush(usuario));
    }

    private void preencherDados(Usuario usuario, UsuarioDTO dto) {

        if (dto.getSenha().getBytes(StandardCharsets.UTF_8).length > 72){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha deve ocupar no máximo 72 bytes em UTF-8");
        }
        usuario.setLogin(dto.getLogin());
        usuario.setEmailRecuperacao(dto.getEmailRecuperacao().strip().toLowerCase(Locale.ROOT));
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setFuncao(dto.getFuncao());
    }

    private Usuario buscarUsuarioOuLancarExcecao(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id));
    }
}
