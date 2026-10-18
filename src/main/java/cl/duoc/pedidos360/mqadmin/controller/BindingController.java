package cl.duoc.pedidos360.mqadmin.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.mqadmin.dto.CreateBindingRequest;
import cl.duoc.pedidos360.mqadmin.dto.Names;
import cl.duoc.pedidos360.mqadmin.dto.Responses.BindingView;
import cl.duoc.pedidos360.mqadmin.service.RabbitAdminService;

@RestController
@RequestMapping("/api/mq/bindings")
@Tag(name = "Bindings")
public class BindingController {

    private final RabbitAdminService admin;

    public BindingController(RabbitAdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    @Operation(summary = "Listar bindings exchange -> cola")
    public List<BindingView> list() {
        return admin.listBindings();
    }

    @PostMapping
    @Operation(summary = "Crear un binding (201). 404 si el exchange o la cola no existen, 409 si ya existe")
    public ResponseEntity<BindingView> create(@Valid @RequestBody CreateBindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(admin.createBinding(request));
    }

    /** Un binding no tiene id propio: se identifica por exchange + cola + routing key. */
    @DeleteMapping
    @Operation(summary = "Eliminar un binding (204). 404 si no existe")
    public ResponseEntity<Void> delete(
            @RequestParam @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE) String exchange,
            @RequestParam @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE) String queue,
            @RequestParam(defaultValue = "")
            @Pattern(regexp = Names.ROUTING_KEY_PATTERN, message = Names.ROUTING_KEY_MESSAGE) String routingKey) {
        admin.deleteBinding(exchange, queue, routingKey);
        return ResponseEntity.noContent().build();
    }
}
