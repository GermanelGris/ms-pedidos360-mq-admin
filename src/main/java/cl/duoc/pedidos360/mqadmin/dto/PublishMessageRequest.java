package cl.duoc.pedidos360.mqadmin.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mensaje de prueba para verificar una ruta (exchange + routing key). Se envía con el mismo envelope que usa el
 * sistema; con {@code "simulateFailure": "transient" | "fatal"} en el payload se prueban reintentos y DLQ.
 */
public record PublishMessageRequest(
        @Schema(example = "cmd.direct")
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.NAME_MAX, message = "admite como máximo " + Names.NAME_MAX + " caracteres")
        @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE)
        String exchange,

        @Schema(example = "email.send")
        @NotNull(message = "es obligatorio")
        @Size(max = Names.ROUTING_KEY_MAX, message = "admite como máximo " + Names.ROUTING_KEY_MAX + " caracteres")
        @Pattern(regexp = Names.ROUTING_KEY_PATTERN, message = Names.ROUTING_KEY_MESSAGE)
        String routingKey,

        @Schema(example = "EmailNotification")
        @Size(max = 100, message = "admite como máximo 100 caracteres")
        String type,

        @Schema(example = "{\"orderId\": 1, \"status\": \"ACEPTADO\", \"customerName\": \"Demo\"}")
        @NotEmpty(message = "es obligatorio y no puede estar vacío")
        Map<String, Object> payload) {
}
