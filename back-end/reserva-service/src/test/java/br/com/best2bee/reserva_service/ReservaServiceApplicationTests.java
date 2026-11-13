package br.com.best2bee.reserva_service;

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
class ReservaServiceApplicationTests {
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
        assertEquals(401, chamar("GET", "/reservas", null).statusCode());
    }

    @Test void corsPermiteOrigemConfiguradaERejeitaOutra() throws Exception {
        var permitido = chamar("OPTIONS", "/reservas", null, "Origin", "http://localhost:5173",
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "authorization");
        assertEquals(200, permitido.statusCode());
        assertEquals("http://localhost:5173", permitido.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals(403, chamar("OPTIONS", "/reservas", null, "Origin", "http://origem-nao-permitida.test",
                "Access-Control-Request-Method", "GET").statusCode());
    }
    @Autowired JdbcTemplate jdbc;

    @Test void bancoImpedeDuasReservasConcorrentesNoMesmoPeriodo() throws Exception {
        int quarto = java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 2000000000);
        var inicio = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<Boolean> inserir = () -> {
            inicio.await();
            try {
                jdbc.update("insert into reserva(id_cliente,id_quarto,data_inicio,data_final,valor_total,preco_diario_aplicado,status,versao) values (1,?,date '2030-01-10',date '2030-01-12',300,150,'PENDENTE',0)", quarto);
                return true;
            } catch (org.springframework.dao.DataAccessException ex) {
                assertInstanceOf(java.sql.SQLException.class, ex.getMostSpecificCause());
                // GiST pode rejeitar por sobreposicao ou abortar uma transacao em deadlock.
                String estado = ((java.sql.SQLException)ex.getMostSpecificCause()).getSQLState();
                assertTrue(java.util.Set.of("23P01", "40P01").contains(estado), estado);
                return false;
            }
        };
        try {
            var a = executor.submit(inserir);
            var b = executor.submit(inserir);
            inicio.countDown();
            assertNotEquals(a.get(15, java.util.concurrent.TimeUnit.SECONDS), b.get(15, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject("select count(*) from reserva where id_quarto=?",Integer.class,quarto));
        } finally {
            executor.shutdownNow();
            jdbc.update("delete from reserva where id_quarto=?",quarto);
        }
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
        assertEquals("hotel_reservas_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from flyway_schema_history where success and type = 'SQL'", Integer.class));
    }
}
