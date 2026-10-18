package cl.duoc.pedidos360.mqadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.mqadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.mqadmin.dto.Responses.PublishResult;
import cl.duoc.pedidos360.mqadmin.service.RabbitAdminService;

@RestController
@RequestMapping("/api/mq/messages")
@Tag(name = "Mensajes de prueba")
public class MessageController {

    private final RabbitAdminService admin;

    public MessageController(RabbitAdminService admin) {
        this.admin = admin;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Publicar un mensaje de prueba para verificar una ruta (202). "
            + "Con payload.simulateFailure = transient | fatal se prueban reintentos y DLQ")
    public PublishResult publish(@Valid @RequestBody PublishMessageRequest request) {
        return admin.publish(request);
    }
}
