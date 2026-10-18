package cl.duoc.pedidos360.mqadmin.controller;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.mqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.mqadmin.dto.Names;
import cl.duoc.pedidos360.mqadmin.dto.Responses.PurgeResult;
import cl.duoc.pedidos360.mqadmin.dto.Responses.QueueView;
import cl.duoc.pedidos360.mqadmin.service.RabbitAdminService;

@RestController
@RequestMapping("/api/mq/queues")
@Tag(name = "Colas")
public class QueueController {

    private final RabbitAdminService admin;

    public QueueController(RabbitAdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    @Operation(summary = "Listar colas con su cantidad de mensajes y consumidores")
    public List<QueueView> list() {
        return admin.listQueues();
    }

    @GetMapping("/{name}")
    @Operation(summary = "Detalle de una cola (404 si no existe)")
    public QueueView get(@PathVariable @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE) String name) {
        return admin.getQueue(name);
    }

    @PostMapping
    @Operation(summary = "Crear una cola (201). 409 si ya existe, 422 si el dead-letter exchange no existe")
    public ResponseEntity<QueueView> create(@Valid @RequestBody CreateQueueRequest request) {
        QueueView queue = admin.createQueue(request);
        return ResponseEntity.created(URI.create("/api/mq/queues/" + queue.name())).body(queue);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Eliminar una cola (204). 404 si no existe, 409 si es parte de la topología del sistema")
    public ResponseEntity<Void> delete(
            @PathVariable @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE) String name) {
        admin.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}/messages")
    @Operation(summary = "Vaciar los mensajes de una cola, por ejemplo una DLQ ya revisada")
    public PurgeResult purge(
            @PathVariable @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE) String name) {
        return admin.purgeQueue(name);
    }
}
