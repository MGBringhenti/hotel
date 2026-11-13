package br.com.best2bee.notificacoes_service;

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

@SpringBootTest
@ActiveProfiles("test")
class NotificacoesServiceApplicationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired br.com.best2bee.notificacoes_service.service.EventoRecebidoService eventos;

    @Test void eventoDuplicadoNaoCriaOutraNotificacaoEReprocessamentoSelecionaSomenteFalhas() throws Exception {
        var id = java.util.UUID.randomUUID();
        int reservaId = java.util.concurrent.ThreadLocalRandom.current().nextInt(100000,2000000000);
        var dados = new br.com.best2bee.notificacoes_service.dto.ReservaEventoDTO(
                1,reservaId,0L,1,2,java.time.LocalDate.of(2030,1,10),java.time.LocalDate.of(2030,1,12),
                new java.math.BigDecimal("150.00"),new java.math.BigDecimal("300.00"),"PENDENTE");
        var evento = new br.com.best2bee.notificacoes_service.dto.EventoKafkaDTO(
                id,"RESERVA_CRIADA",java.time.Instant.now(),dados);
        assertTrue(eventos.registrar(evento));
        assertFalse(eventos.registrar(evento));
        assertEquals(1, jdbc.queryForObject("select count(*) from notificacao where evento_id=?",Integer.class,id));
        jdbc.update("update notificacao set tentativas=10, descartado_em=now(), motivo_descarte='LIMITE_TENTATIVAS' where evento_id=?",id);
        String arquivo = java.nio.file.Files.readString(java.nio.file.Path.of("..","docs","sql","reprocessar_notificacao.sql"));
        String sql = arquivo.substring(arquivo.indexOf("WITH alvo AS"))
                .replace("SELECT '00000000-0000-0000-0000-000000000000'", "SELECT '" + id + "'");
        assertEquals(1, jdbc.queryForList(sql).size());
        assertEquals(0, jdbc.queryForObject("select tentativas from notificacao where evento_id=?",Integer.class,id));
        assertTrue(jdbc.queryForList(sql).isEmpty());
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
        assertEquals("hotel_notificacoes_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from flyway_schema_history where success and type = 'SQL'", Integer.class));
    }
}
