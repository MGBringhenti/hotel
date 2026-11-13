package br.com.best2bee.cliente_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.security.KeyPairGenerator;
import java.nio.file.Files;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ClienteServiceApplicationTests {
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;

    java.net.http.HttpResponse<String> chamar(String metodo, String rota, String corpo, String... headers) throws Exception {
        var builder = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + port + rota))
                .timeout(java.time.Duration.ofSeconds(10));
        if (headers.length > 0) builder.headers(headers);
        builder.method(metodo, corpo == null ? java.net.http.HttpRequest.BodyPublishers.noBody()
                : java.net.http.HttpRequest.BodyPublishers.ofString(corpo));
        return java.net.http.HttpClient.newHttpClient().send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    @Test void rotaProtegidaExigeToken() throws Exception {
        assertEquals(401, chamar("GET", "/usuarios", null).statusCode());
    }

    @Test void corsPermiteOrigemConfiguradaERejeitaOutra() throws Exception {
        var permitido = chamar("OPTIONS", "/usuarios", null, "Origin", "http://localhost:5173",
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "authorization");
        assertEquals(200, permitido.statusCode());
        assertEquals("http://localhost:5173", permitido.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals(403, chamar("OPTIONS", "/usuarios", null, "Origin", "http://origem-nao-permitida.test",
                "Access-Control-Request-Method", "GET").statusCode());
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired tools.jackson.databind.json.JsonMapper json;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;

    @Test void loginRedefinicaoEPoliticaDoJwtFuncionamPorHttp() throws Exception {
        String login = "teste-" + java.util.UUID.randomUUID();
        String senha = "SenhaInicial123";
        Integer usuarioId = jdbc.queryForObject(
                "insert into usuarios(login, senha, funcao, ativo, versao, email_recuperacao) values (?, ?, 'ADMIN', true, 0, ?) returning id",
                Integer.class, login, passwords.encode(senha), login + "@hotel.test");
        var loginResposta = chamar("POST", "/auth/login", json.writeValueAsString(java.util.Map.of("login",login,"senha",senha)),
                "Content-Type","application/json");
        assertEquals(200, loginResposta.statusCode());
        String jwt = (String) json.readValue(loginResposta.body(), java.util.Map.class).get("accessToken");
        assertNotNull(jwt);
        assertEquals(200, chamar("GET", "/usuarios", null, "Authorization","Bearer " + jwt).statusCode());
        byte[] aleatorio = new byte[32];
        new java.security.SecureRandom().nextBytes(aleatorio);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        jdbc.update("insert into recuperacao_senha(id, usuario_id, usuario_versao, token_hash, criado_em, expira_em) values (?, ?, 0, ?, now(), now() + interval '30 minutes')",
                java.util.UUID.randomUUID(), usuarioId, hash);
        String nova = "SenhaNova456";
        String redefinicao = json.writeValueAsString(java.util.Map.of("token",token,"senhaNova",nova));
        assertEquals(204, chamar("POST","/auth/recuperacao-senha/redefinir",redefinicao,"Content-Type","application/json").statusCode());
        assertEquals(400, chamar("POST","/auth/recuperacao-senha/redefinir",redefinicao,"Content-Type","application/json").statusCode());
        assertEquals(401, chamar("POST","/auth/login",json.writeValueAsString(java.util.Map.of("login",login,"senha",senha)),"Content-Type","application/json").statusCode());
        assertEquals(200, chamar("POST","/auth/login",json.writeValueAsString(java.util.Map.of("login",login,"senha",nova)),"Content-Type","application/json").statusCode());
        assertEquals(200, chamar("GET","/usuarios",null,"Authorization","Bearer " + jwt).statusCode());
    }

    @DynamicPropertySource
    static void chavesTemporarias(DynamicPropertyRegistry props) throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keys = generator.generateKeyPair();
        var dir = Files.createTempDirectory("hotel-test-keys-");
        var pub = dir.resolve("public.pem");
        var priv = dir.resolve("private.pem");
        var encoder = Base64.getMimeEncoder(64, new byte[]{10});
        Files.writeString(pub, "-----BEGIN PUBLIC KEY-----\n" + encoder.encodeToString(keys.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n");
        Files.writeString(priv, "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(keys.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n");
        dir.toFile().deleteOnExit(); pub.toFile().deleteOnExit(); priv.toFile().deleteOnExit();
        props.add("jwt.public-key", () -> pub.toUri().toString());
        props.add("jwt.private-key", () -> priv.toUri().toString());
    }

    @Test void contextLoadsComMigracoesAplicadasEmBancoIsolado() {
        assertEquals("hotel_clientes_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(5, jdbc.queryForObject("select count(*) from flyway_schema_history where success and type = 'SQL'", Integer.class));
    }
}
