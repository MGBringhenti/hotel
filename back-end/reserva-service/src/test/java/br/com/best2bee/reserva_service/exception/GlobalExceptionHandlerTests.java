package br.com.best2bee.reserva_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTests {
    @RestController
    static class OperacaoConcorrente {
        @GetMapping("/conflito")
        void executar() {
            throw new CannotAcquireLockException("Detalhes internos do PostgreSQL");
        }
    }
    @Test void conflitoDeBloqueioRetorna409SemExporDetalhesDoBanco() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new OperacaoConcorrente())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value(
                        "Outra operação está alterando os dados da reserva. Consulte a disponibilidade e tente novamente."));
    }
}
