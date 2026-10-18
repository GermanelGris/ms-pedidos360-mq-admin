package cl.duoc.pedidos360.mqadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Une una cola a un exchange con una routing key (puede ser vacía para exchanges fanout). */
public record CreateBindingRequest(
        @Schema(example = "ex.demo")
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.NAME_MAX, message = "admite como máximo " + Names.NAME_MAX + " caracteres")
        @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE)
        String exchange,

        @Schema(example = "q.demo")
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.NAME_MAX, message = "admite como máximo " + Names.NAME_MAX + " caracteres")
        @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE)
        String queue,

        @Schema(example = "demo.created")
        @NotNull(message = "es obligatorio (use \"\" para exchanges fanout)")
        @Size(max = Names.ROUTING_KEY_MAX, message = "admite como máximo " + Names.ROUTING_KEY_MAX + " caracteres")
        @Pattern(regexp = Names.ROUTING_KEY_PATTERN, message = Names.ROUTING_KEY_MESSAGE)
        String routingKey) {
}
