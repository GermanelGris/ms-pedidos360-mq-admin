package cl.duoc.pedidos360.mqadmin.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.mqadmin.client.RabbitManagementClient;
import cl.duoc.pedidos360.mqadmin.config.AdminProperties;
import cl.duoc.pedidos360.mqadmin.dto.CreateBindingRequest;
import cl.duoc.pedidos360.mqadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.mqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.mqadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.mqadmin.dto.Responses.BindingView;
import cl.duoc.pedidos360.mqadmin.dto.Responses.ExchangeView;
import cl.duoc.pedidos360.mqadmin.dto.Responses.PublishResult;
import cl.duoc.pedidos360.mqadmin.dto.Responses.PurgeResult;
import cl.duoc.pedidos360.mqadmin.dto.Responses.QueueView;
import cl.duoc.pedidos360.mqadmin.exception.AdminException;

/**
 * Toda la administración de RabbitMQ pasa por aquí: los controladores solo llaman a estos métodos y no conocen
 * AmqpAdmin, RabbitTemplate ni la API de administración. Antes de cada operación se validan las reglas que
 * RabbitMQ no controla por sí mismo (existencia previa, recursos protegidos, referencias válidas).
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbit;
    private final RabbitManagementClient management;
    private final AdminProperties properties;
    private final ObjectMapper mapper;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitTemplate rabbit, RabbitManagementClient management,
                              AdminProperties properties, ObjectMapper mapper) {
        this.amqpAdmin = amqpAdmin;
        this.rabbit = rabbit;
        this.management = management;
        this.properties = properties;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- Colas

    public List<QueueView> listQueues() {
        return management.queues().stream()
                .map(q -> new QueueView(q.name(), q.durable(), q.messages(), q.consumers(), q.arguments(),
                        properties.protectedQueues().contains(q.name())))
                .sorted(Comparator.comparing(QueueView::name))
                .toList();
    }

    public QueueView getQueue(String name) {
        QueueInformation info = requireQueue(name);
        return new QueueView(info.getName(), true, info.getMessageCount(), info.getConsumerCount(), Map.of(),
                properties.protectedQueues().contains(name));
    }

    public QueueView createQueue(CreateQueueRequest request) {
        if (amqpAdmin.getQueueInfo(request.name()) != null) {
            throw AdminException.conflict("La cola '" + request.name() + "' ya existe");
        }
        QueueBuilder builder = request.isDurable()
                ? QueueBuilder.durable(request.name())
                : QueueBuilder.nonDurable(request.name());
        if (hasText(request.deadLetterExchange())) {
            if (!management.exchangeExists(request.deadLetterExchange())) {
                throw AdminException.invalid("El dead-letter exchange '" + request.deadLetterExchange()
                        + "' no existe: créelo antes de usarlo");
            }
            builder.deadLetterExchange(request.deadLetterExchange());
            if (request.deadLetterRoutingKey() != null) {
                builder.deadLetterRoutingKey(request.deadLetterRoutingKey());
            }
        }
        if (request.messageTtlMs() != null) {
            builder.ttl(request.messageTtlMs());
        }
        if (request.maxLength() != null) {
            builder.maxLength(request.maxLength());
        }
        Queue queue = builder.build();
        amqpAdmin.declareQueue(queue);
        log.info("Cola creada: {} {}", queue.getName(), queue.getArguments());
        return new QueueView(queue.getName(), queue.isDurable(), 0, 0, queue.getArguments(), false);
    }

    public void deleteQueue(String name) {
        if (properties.protectedQueues().contains(name)) {
            throw AdminException.conflict("La cola '" + name + "' es parte de la topología del sistema y no se "
                    + "puede eliminar");
        }
        requireQueue(name);
        amqpAdmin.deleteQueue(name);
        log.info("Cola eliminada: {}", name);
    }

    public PurgeResult purgeQueue(String name) {
        requireQueue(name);
        int purged = amqpAdmin.purgeQueue(name);
        log.info("Cola vaciada: {} ({} mensajes)", name, purged);
        return new PurgeResult(name, purged);
    }

    // ---------------------------------------------------------------- Exchanges

    public List<ExchangeView> listExchanges() {
        return management.exchanges().stream()
                .filter(e -> hasText(e.name()) && !e.name().startsWith("amq."))
                .map(e -> new ExchangeView(e.name(), e.type(), e.durable(),
                        properties.protectedExchanges().contains(e.name())))
                .sorted(Comparator.comparing(ExchangeView::name))
                .toList();
    }

    public ExchangeView createExchange(CreateExchangeRequest request) {
        if (management.exchangeExists(request.name())) {
            throw AdminException.conflict("El exchange '" + request.name() + "' ya existe");
        }
        Exchange exchange = new ExchangeBuilder(request.name(), request.type().value())
                .durable(request.isDurable())
                .build();
        amqpAdmin.declareExchange(exchange);
        log.info("Exchange creado: {} ({})", exchange.getName(), exchange.getType());
        return new ExchangeView(exchange.getName(), exchange.getType(), exchange.isDurable(), false);
    }

    public void deleteExchange(String name) {
        if (properties.protectedExchanges().contains(name)) {
            throw AdminException.conflict("El exchange '" + name + "' es parte de la topología del sistema y no "
                    + "se puede eliminar");
        }
        requireExchange(name);
        amqpAdmin.deleteExchange(name);
        log.info("Exchange eliminado: {}", name);
    }

    // ---------------------------------------------------------------- Bindings

    public List<BindingView> listBindings() {
        return management.bindings().stream()
                .filter(b -> hasText(b.source()) && "queue".equals(b.destinationType()))
                .map(b -> new BindingView(b.source(), b.destination(), b.routingKey()))
                .sorted(Comparator.comparing(BindingView::exchange).thenComparing(BindingView::queue))
                .toList();
    }

    public BindingView createBinding(CreateBindingRequest request) {
        requireExchange(request.exchange());
        requireQueue(request.queue());
        BindingView binding = new BindingView(request.exchange(), request.queue(), request.routingKey());
        if (listBindings().contains(binding)) {
            throw AdminException.conflict("El binding " + describe(binding) + " ya existe");
        }
        amqpAdmin.declareBinding(toBinding(binding));
        log.info("Binding creado: {}", describe(binding));
        return binding;
    }

    public void deleteBinding(String exchange, String queue, String routingKey) {
        BindingView binding = new BindingView(exchange, queue, routingKey);
        if (!listBindings().contains(binding)) {
            throw AdminException.notFound("El binding " + describe(binding) + " no existe");
        }
        amqpAdmin.removeBinding(toBinding(binding));
        log.info("Binding eliminado: {}", describe(binding));
    }

    // ---------------------------------------------------------------- Mensajes de prueba

    public PublishResult publish(PublishMessageRequest request) {
        requireExchange(request.exchange());
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", hasText(request.type()) ? request.type() : "TestMessage");
        envelope.put("eventId", eventId);
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("traceId", UUID.randomUUID().toString());
        envelope.put("correlationId", "mq-admin");
        envelope.put("payload", request.payload());
        rabbit.send(request.exchange(), request.routingKey(), MessageBuilder.withBody(mapper.writeValueAsBytes(envelope))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(eventId)
                .build());
        log.info("Mensaje de prueba {} publicado en {} con routing key '{}'", eventId, request.exchange(),
                request.routingKey());
        return new PublishResult(eventId, request.exchange(), request.routingKey());
    }

    // ---------------------------------------------------------------- Apoyo

    private QueueInformation requireQueue(String name) {
        QueueInformation info = amqpAdmin.getQueueInfo(name);
        if (info == null) {
            throw AdminException.notFound("La cola '" + name + "' no existe");
        }
        return info;
    }

    private void requireExchange(String name) {
        if (!management.exchangeExists(name)) {
            throw AdminException.notFound("El exchange '" + name + "' no existe");
        }
    }

    private static Binding toBinding(BindingView binding) {
        return new Binding(binding.queue(), DestinationType.QUEUE, binding.exchange(), binding.routingKey(), null);
    }

    private static String describe(BindingView binding) {
        return binding.exchange() + " --[" + binding.routingKey() + "]--> " + binding.queue();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
