package cl.duoc.pedidos360.mqadmin.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Respuestas de error uniformes: {timestamp, status, error, message, path, details}.
 * 400 entrada inválida · 404 recurso inexistente · 409 conflicto · 422 configuración inválida · 503 broker caído.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AdminException.class)
    public ResponseEntity<Map<String, Object>> handleAdmin(AdminException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBody(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<String> details = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage()),
                        ex.getBindingResult().getGlobalErrors().stream().map(error -> error.getDefaultMessage()))
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La petición tiene datos inválidos", details, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleParameters(HandlerMethodValidationException ex,
                                                                HttpServletRequest request) {
        List<String> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": "
                                + error.getDefaultMessage()))
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La petición tiene parámetros inválidos", details, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissing(MissingServletRequestParameterException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + ex.getParameterName() + "'",
                List.of(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        String detail = cause instanceof IllegalArgumentException ? cause.getMessage() : "JSON mal formado";
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido", List.of(detail), request);
    }

    @ExceptionHandler({AmqpConnectException.class, ResourceAccessException.class})
    public ResponseEntity<Map<String, Object>> handleBrokerDown(Exception ex, HttpServletRequest request) {
        log.error("RabbitMQ no disponible: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no está disponible", List.of(), request);
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> handleAmqp(AmqpException ex, HttpServletRequest request) {
        log.error("RabbitMQ rechazó la operación: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "RabbitMQ rechazó la operación",
                List.of(String.valueOf(rootCause(ex).getMessage())), request);
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, List<String> details,
                                                      HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        if (!details.isEmpty()) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
