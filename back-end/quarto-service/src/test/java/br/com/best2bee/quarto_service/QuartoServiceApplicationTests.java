package br.com.best2bee.quarto_service;

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
class QuartoServiceApplicationTests {
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
        assertEquals(401, chamar("GET", "/quarto", null).statusCode());
    }

    @Test void corsPermiteOrigemConfiguradaERejeitaOutra() throws Exception {
        var permitido = chamar("OPTIONS", "/quarto", null, "Origin", "http://localhost:5173",
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "authorization");
        assertEquals(200, permitido.statusCode());
        assertEquals("http://localhost:5173", permitido.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals(403, chamar("OPTIONS", "/quarto", null, "Origin", "http://origem-nao-permitida.test",
                "Access-Control-Request-Method", "GET").statusCode());
    }
    @Autowired JdbcTemplate jdbc;

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
        assertEquals("hotel_quartos_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(6, jdbc.queryForObject("select count(*) from flyway_schema_history where success and type = 'SQL'", Integer.class));
    }
}
