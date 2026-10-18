package cl.duoc.pedidos360.mqadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Datos para crear una cola. Solo {@code name} es obligatorio. */
public record CreateQueueRequest(
        @Schema(example = "q.demo")
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.NAME_MAX, message = "admite como máximo " + Names.NAME_MAX + " caracteres")
        @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE)
        String name,

        @Schema(description = "Sobrevive a reinicios del broker. Por defecto true", example = "true")
        Boolean durable,

        @Schema(description = "Exchange al que se envían los mensajes rechazados (debe existir)", example = "cmd.dead.dlx")
        @Size(max = Names.NAME_MAX, message = "admite como máximo " + Names.NAME_MAX + " caracteres")
        @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE)
        String deadLetterExchange,

        @Schema(example = "demo")
        @Size(max = Names.ROUTING_KEY_MAX, message = "admite como máximo " + Names.ROUTING_KEY_MAX + " caracteres")
        @Pattern(regexp = Names.ROUTING_KEY_PATTERN, message = Names.ROUTING_KEY_MESSAGE)
        String deadLetterRoutingKey,

        @Schema(description = "Tiempo de vida de cada mensaje en ms (máx. 24 h)", example = "60000")
        @Positive(message = "debe ser mayor que 0")
        @Max(value = 86_400_000, message = "no puede superar 86400000 ms (24 horas)")
        Integer messageTtlMs,

        @Schema(description = "Cantidad máxima de mensajes en la cola", example = "1000")
        @Positive(message = "debe ser mayor que 0")
        @Max(value = 1_000_000, message = "no puede superar 1000000 mensajes")
        Integer maxLength) {

    public boolean isDurable() {
        return durable == null || durable;
    }

    @Schema(hidden = true)
    @AssertTrue(message = "deadLetterRoutingKey requiere indicar deadLetterExchange")
    public boolean isDeadLetterConsistent() {
        return deadLetterRoutingKey == null || deadLetterRoutingKey.isEmpty()
                || (deadLetterExchange != null && !deadLetterExchange.isBlank());
    }
}
