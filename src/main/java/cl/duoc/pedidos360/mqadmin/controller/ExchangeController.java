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

import cl.duoc.pedidos360.mqadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.mqadmin.dto.Names;
import cl.duoc.pedidos360.mqadmin.dto.Responses.ExchangeView;
import cl.duoc.pedidos360.mqadmin.service.RabbitAdminService;

@RestController
@RequestMapping("/api/mq/exchanges")
@Tag(name = "Exchanges")
public class ExchangeController {

    private final RabbitAdminService admin;

    public ExchangeController(RabbitAdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    @Operation(summary = "Listar exchanges (sin los internos amq.*)")
    public List<ExchangeView> list() {
        return admin.listExchanges();
    }

    @PostMapping
    @Operation(summary = "Crear un exchange direct, topic, fanout o headers (201). 409 si ya existe")
    public ResponseEntity<ExchangeView> create(@Valid @RequestBody CreateExchangeRequest request) {
        ExchangeView exchange = admin.createExchange(request);
        return ResponseEntity.created(URI.create("/api/mq/exchanges/" + exchange.name())).body(exchange);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Eliminar un exchange (204). 404 si no existe, 409 si es parte de la topología del sistema")
    public ResponseEntity<Void> delete(
            @PathVariable @Pattern(regexp = Names.NAME_PATTERN, message = Names.NAME_MESSAGE) String name) {
        admin.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }
}
