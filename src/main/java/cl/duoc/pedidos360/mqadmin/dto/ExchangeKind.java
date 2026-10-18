package cl.duoc.pedidos360.mqadmin.dto;

import java.util.Arrays;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Tipos de exchange soportados por RabbitMQ. En JSON se escriben en minúscula: "direct", "topic"... */
public enum ExchangeKind {
    DIRECT, TOPIC, FANOUT, HEADERS;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static ExchangeKind from(String value) {
        return Arrays.stream(values())
                .filter(kind -> kind.name().equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "type debe ser direct, topic, fanout o headers (recibido: " + value + ")"));
    }
}
