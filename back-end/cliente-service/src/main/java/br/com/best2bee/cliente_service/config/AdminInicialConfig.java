package br.com.best2bee.cliente_service.config;

import br.com.best2bee.cliente_service.enums.Funcao;
import br.com.best2bee.cliente_service.model.Usuario;
import br.com.best2bee.cliente_service.repository.UsuarioRepository;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration
@Profile("bootstrap-admin")
public class AdminInicialConfig {

    @Bean
    CommandLineRunner criarAdminInicial(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            Validator validator,
            @Value("${admin.inicial.login}") String login,
            @Value("${admin.inicial.senha}") String senha,
            @Value("${admin.inicial.email}") String email) {

        return args -> {
            if (login == null
                    || login.isBlank()
                    || login.length() > 75) {

                throw new IllegalStateException(
                        "Configure ADMIN_INICIAL_LOGIN com até 75 caracteres"
                );
            }

            var existente = repository.findByLogin(login);

            if (existente.isPresent()) {
                var usuario = existente.get();

                if (usuario.getFuncao() != Funcao.ADMIN
                        || !usuario.isAtivo()) {

                    throw new IllegalStateException(
                            "O login já pertence a uma conta "
                                    + "que não é um administrador ativo"
                    );
                }

                return;
            }

            if (senha == null
                    || senha.isBlank()
                    || senha.length() < 8
                    || senha.length() > 50
                    || senha.getBytes(StandardCharsets.UTF_8).length > 72) {

                throw new IllegalStateException(
                        "Configure ADMIN_INICIAL_SENHA com 8 a 50 "
                                + "caracteres e no máximo 72 bytes em UTF-8"
                );
            }

            String emailNormalizado = email == null
                    ? ""
                    : email.strip().toLowerCase(Locale.ROOT);

            var erros = validator.validate(
                    new EmailInicial(emailNormalizado)
            );

            if (!erros.isEmpty()) {
                throw new IllegalStateException(
                        "Configure ADMIN_INICIAL_EMAIL com um e-mail "
                                + "válido de até 254 caracteres"
                );
            }

            if (repository.findByEmailRecuperacao(
                    emailNormalizado).isPresent()) {

                throw new IllegalStateException(
                        "O e-mail de recuperação informado já está em uso"
                );
            }

            Usuario admin = new Usuario();

            admin.setLogin(login);
            admin.setSenha(passwordEncoder.encode(senha));
            admin.setFuncao(Funcao.ADMIN);
            admin.setEmailRecuperacao(emailNormalizado);

            repository.save(admin);
        };
    }

    private record EmailInicial(
            @NotBlank
            @Email
            @Size(max = 254)
            String email
    ) {}
}