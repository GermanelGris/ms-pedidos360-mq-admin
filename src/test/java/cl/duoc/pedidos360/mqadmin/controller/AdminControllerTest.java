package cl.duoc.pedidos360.mqadmin.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import cl.duoc.pedidos360.mqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.mqadmin.dto.Responses.BindingView;
import cl.duoc.pedidos360.mqadmin.dto.Responses.ExchangeView;
import cl.duoc.pedidos360.mqadmin.dto.Responses.QueueView;
import cl.duoc.pedidos360.mqadmin.exception.AdminException;
import cl.duoc.pedidos360.mqadmin.service.RabbitAdminService;

@WebMvcTest(controllers = {QueueController.class, ExchangeController.class, BindingController.class,
        MessageController.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RabbitAdminService admin;

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // ---------------------------------------------------------------- Colas

    @Test
    void crearColaValidaResponde201ConLocation() throws Exception {
        given(admin.createQueue(any(CreateQueueRequest.class)))
                .willReturn(new QueueView("q.demo", true, 0, 0, Map.of(), false));

        postJson("/api/mq/queues", "{\"name\":\"q.demo\",\"messageTtlMs\":60000}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/mq/queues/q.demo"))
                .andExpect(jsonPath("$.name").value("q.demo"));
    }

    @Test
    void colaConNombreVacioResponde400() throws Exception {
        postJson("/api/mq/queues", "{\"name\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("name: es obligatorio")));
        verifyNoInteractions(admin);
    }

    @Test
    void colaConNombreReservadoOCaracteresInvalidosResponde400() throws Exception {
        postJson("/api/mq/queues", "{\"name\":\"amq.mia\"}").andExpect(status().isBadRequest());
        postJson("/api/mq/queues", "{\"name\":\"cola con espacios\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(admin);
    }

    @Test
    void colaConTtlNegativoOExcesivoResponde400() throws Exception {
        postJson("/api/mq/queues", "{\"name\":\"q.demo\",\"messageTtlMs\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("messageTtlMs: debe ser mayor que 0")));
        postJson("/api/mq/queues", "{\"name\":\"q.demo\",\"messageTtlMs\":999999999}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(admin);
    }

    @Test
    void deadLetterRoutingKeySinExchangeResponde400() throws Exception {
        postJson("/api/mq/queues", "{\"name\":\"q.demo\",\"deadLetterRoutingKey\":\"demo\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("deadLetterConsistent: "
                        + "deadLetterRoutingKey requiere indicar deadLetterExchange"));
        verifyNoInteractions(admin);
    }

    @Test
    void colaDuplicadaResponde409() throws Exception {
        given(admin.createQueue(any(CreateQueueRequest.class)))
                .willThrow(AdminException.conflict("La cola 'q.demo' ya existe"));

        postJson("/api/mq/queues", "{\"name\":\"q.demo\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La cola 'q.demo' ya existe"));
    }

    @Test
    void eliminarColaResponde204() throws Exception {
        mvc.perform(delete("/api/mq/queues/q.demo")).andExpect(status().isNoContent());
        verify(admin).deleteQueue("q.demo");
    }

    @Test
    void eliminarColaInexistenteResponde404() throws Exception {
        willThrow(AdminException.notFound("La cola 'q.nada' no existe")).given(admin).deleteQueue("q.nada");

        mvc.perform(delete("/api/mq/queues/q.nada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void brokerCaidoResponde503() throws Exception {
        given(admin.listQueues()).willThrow(new AmqpConnectException(new RuntimeException("Connection refused")));

        mvc.perform(get("/api/mq/queues"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("RabbitMQ no está disponible"));
    }

    // ---------------------------------------------------------------- Exchanges

    @Test
    void crearExchangeAceptaElTipoEnMinuscula() throws Exception {
        given(admin.createExchange(any())).willReturn(new ExchangeView("ex.demo", "topic", true, false));

        postJson("/api/mq/exchanges", "{\"name\":\"ex.demo\",\"type\":\"topic\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("topic"));
    }

    @Test
    void exchangeSinTipoOConTipoInvalidoResponde400() throws Exception {
        postJson("/api/mq/exchanges", "{\"name\":\"ex.demo\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("type: es obligatorio (direct, topic, fanout o headers)")));
        postJson("/api/mq/exchanges", "{\"name\":\"ex.demo\",\"type\":\"cualquiera\"}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(admin);
    }

    // ---------------------------------------------------------------- Bindings y mensajes

    @Test
    void crearBindingResponde201() throws Exception {
        given(admin.createBinding(any())).willReturn(new BindingView("ex.demo", "q.demo", "demo.*"));

        postJson("/api/mq/bindings", "{\"exchange\":\"ex.demo\",\"queue\":\"q.demo\",\"routingKey\":\"demo.*\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routingKey").value("demo.*"));
    }

    @Test
    void bindingSinRoutingKeyResponde400() throws Exception {
        postJson("/api/mq/bindings", "{\"exchange\":\"ex.demo\",\"queue\":\"q.demo\"}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(admin);
    }

    @Test
    void eliminarBindingPorParametros() throws Exception {
        mvc.perform(delete("/api/mq/bindings").param("exchange", "ex.demo").param("queue", "q.demo")
                        .param("routingKey", "demo.*"))
                .andExpect(status().isNoContent());
        verify(admin).deleteBinding("ex.demo", "q.demo", "demo.*");
    }

    @Test
    void eliminarBindingConParametroInvalidoResponde400() throws Exception {
        mvc.perform(delete("/api/mq/bindings").param("exchange", "amq.direct").param("queue", "q.demo"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(admin);
    }

    @Test
    void mensajeSinPayloadResponde400() throws Exception {
        postJson("/api/mq/messages", "{\"exchange\":\"cmd.direct\",\"routingKey\":\"email.send\",\"payload\":{}}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(admin);
    }
}
