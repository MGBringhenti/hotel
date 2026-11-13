package br.com.best2bee.quarto_service.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ErroResposta erro = new ErroResposta(HttpStatus.NOT_FOUND.value(), "Recurso não encontrado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErroResposta> handleConflitoVersao(
            OptimisticLockingFailureException ex) {

        var erro = new ErroResposta(
                HttpStatus.CONFLICT.value(),
                "Conflito de atualização",
                "O cadastro foi alterado por outra operação. Consulte os dados novamente.");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> handleCorpoInvalido(
            HttpMessageNotReadableException ex) {

        var erro = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                "Corpo da requisição inválido",
                "Verifique o JSON, os tipos dos campos e os valores permitidos.");

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> handleParametroInvalido(
            MethodArgumentTypeMismatchException ex) {

        var erro = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                "Parâmetro inválido",
                "O parâmetro '" + ex.getName() + "' possui um valor incompatível.");

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> handleIntegridade(
            DataIntegrityViolationException ex) {

        ErroResposta erro = new ErroResposta(
                HttpStatus.CONFLICT.value(),
                "Conflito nos dados",
                "A operação viola uma restrição de integridade. "
                        + "Verifique se o número do quarto já existe "
                        + "ou se há vínculos que impedem a operação.");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> handleValidacao(MethodArgumentNotValidException ex) {
        String mensagens = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErroResposta erro = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de Validação",
                mensagens);
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResposta> handleResponseStatus(
            ResponseStatusException ex) {

        ErroResposta erro = new ErroResposta(
                ex.getStatusCode().value(),
                "Erro na operação",
                ex.getReason());

        return ResponseEntity.status(ex.getStatusCode()).body(erro);
    }
}
