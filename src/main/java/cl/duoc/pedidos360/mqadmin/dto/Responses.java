package cl.duoc.pedidos360.mqadmin.dto;

import java.util.Map;

/** Respuestas de la API del administrador. */
public final class Responses {

    private Responses() {
    }

    public record QueueView(String name, boolean durable, long messages, long consumers,
                            Map<String, Object> arguments, boolean protectedResource) {
    }

    public record ExchangeView(String name, String type, boolean durable, boolean protectedResource) {
    }

    public record BindingView(String exchange, String queue, String routingKey) {
    }

    public record PurgeResult(String queue, int purgedMessages) {
    }

    public record PublishResult(String eventId, String exchange, String routingKey) {
    }
}
