package cl.duoc.pedidos360.mqadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear un exchange. */
public record CreateExchangeRequest(
        @Schema(example = "ex.demo")
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.NAME_MAX, message = "admite como máximo " + Names.NAME_MAX + " caracteres")
        @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE)
        String name,

        @Schema(description = "direct, topic, fanout o headers", example = "direct")
        @NotNull(message = "es obligatorio (direct, topic, fanout o headers)")
        ExchangeKind type,

        @Schema(description = "Sobrevive a reinicios del broker. Por defecto true", example = "true")
        Boolean durable) {

    public boolean isDurable() {
        return durable == null || durable;
    }
}
