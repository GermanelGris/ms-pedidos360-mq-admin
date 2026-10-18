package cl.duoc.pedidos360.mqadmin.config;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuración del administrador, leída de {@code pedidos360.admin} en application.yml. */
@Validated
@ConfigurationProperties("pedidos360.admin")
public record AdminProperties(
        @NotBlank String managementUrl,
        Set<String> protectedExchanges,
        Set<String> protectedQueues) {

    public AdminProperties {
        protectedExchanges = protectedExchanges == null ? Set.of() : Set.copyOf(protectedExchanges);
        protectedQueues = protectedQueues == null ? Set.of() : Set.copyOf(protectedQueues);
    }
}
