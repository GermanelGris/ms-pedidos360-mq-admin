package cl.duoc.pedidos360.mqadmin.client;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.pedidos360.mqadmin.config.AdminProperties;

/**
 * Lectura de recursos mediante la API HTTP del plugin rabbitmq_management. AMQP permite declarar y eliminar,
 * pero no listar, por eso las consultas usan esta API. Solo la utiliza {@code RabbitAdminService}.
 */
@Component
public class RabbitManagementClient {

    private final RestClient http;
    private final String vhost;

    public RabbitManagementClient(AdminProperties admin, RabbitProperties rabbit) {
        this.vhost = rabbit.getVirtualHost() == null ? "/" : rabbit.getVirtualHost();
        this.http = RestClient.builder()
                .baseUrl(admin.managementUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(rabbit.getUsername(), rabbit.getPassword()))
                .build();
    }

    public List<QueueDetails> queues() {
        return http.get().uri("/api/queues/{vhost}", vhost).retrieve()
                .body(new ParameterizedTypeReference<List<QueueDetails>>() { });
    }

    public List<ExchangeDetails> exchanges() {
        return http.get().uri("/api/exchanges/{vhost}", vhost).retrieve()
                .body(new ParameterizedTypeReference<List<ExchangeDetails>>() { });
    }

    public List<BindingDetails> bindings() {
        return http.get().uri("/api/bindings/{vhost}", vhost).retrieve()
                .body(new ParameterizedTypeReference<List<BindingDetails>>() { });
    }

    public boolean exchangeExists(String name) {
        return http.get().uri("/api/exchanges/{vhost}/{name}", vhost, name)
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful()
                        ? Boolean.TRUE
                        : handleMissing(response.getStatusCode().value()));
    }

    private static Boolean handleMissing(int status) {
        if (status == HttpStatus.NOT_FOUND.value()) {
            return Boolean.FALSE;
        }
        throw new IllegalStateException("La API de administración de RabbitMQ respondió " + status);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueueDetails(String name, boolean durable, long messages, long consumers,
                               Map<String, Object> arguments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExchangeDetails(String name, String type, boolean durable, boolean internal) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BindingDetails(String source, String destination,
                                 @JsonProperty("destination_type") String destinationType,
                                 @JsonProperty("routing_key") String routingKey) {
    }
}
