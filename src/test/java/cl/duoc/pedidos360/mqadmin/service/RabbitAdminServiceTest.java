package cl.duoc.pedidos360.mqadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

import tools.jackson.databind.json.JsonMapper;

import cl.duoc.pedidos360.mqadmin.client.RabbitManagementClient;
import cl.duoc.pedidos360.mqadmin.client.RabbitManagementClient.BindingDetails;
import cl.duoc.pedidos360.mqadmin.config.AdminProperties;
import cl.duoc.pedidos360.mqadmin.dto.CreateBindingRequest;
import cl.duoc.pedidos360.mqadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.mqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.mqadmin.dto.ExchangeKind;
import cl.duoc.pedidos360.mqadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.mqadmin.dto.Responses.PublishResult;
import cl.duoc.pedidos360.mqadmin.exception.AdminException;

class RabbitAdminServiceTest {

    private final AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitManagementClient management = mock(RabbitManagementClient.class);
    private final RabbitAdminService service = new RabbitAdminService(amqpAdmin, rabbit, management,
            new AdminProperties("http://localhost:15672", Set.of("cmd.direct"), Set.of("q.cmd.email")),
            JsonMapper.builder().build());

    private static HttpStatus statusOf(Throwable ex) {
        return ((AdminException) ex).getStatus();
    }

    @Test
    void creaColaConDeadLetterYTtl() {
        given(management.exchangeExists("cmd.dead.dlx")).willReturn(true);

        service.createQueue(new CreateQueueRequest("q.demo", null, "cmd.dead.dlx", "demo", 60000, null));

        ArgumentCaptor<Queue> queue = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(queue.capture());
        assertThat(queue.getValue().isDurable()).isTrue();
        assertThat(queue.getValue().getArguments())
                .containsEntry("x-dead-letter-exchange", "cmd.dead.dlx")
                .containsEntry("x-dead-letter-routing-key", "demo")
                .containsEntry("x-message-ttl", 60000);
    }

    @Test
    void noCreaUnaColaQueYaExiste() {
        given(amqpAdmin.getQueueInfo("q.demo")).willReturn(new QueueInformation("q.demo", 0, 0));

        assertThatThrownBy(() -> service.createQueue(new CreateQueueRequest("q.demo", true, null, null, null, null)))
                .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(HttpStatus.CONFLICT));
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void rechazaDeadLetterExchangeInexistente() {
        given(management.exchangeExists("no.existe")).willReturn(false);

        assertThatThrownBy(() -> service.createQueue(
                new CreateQueueRequest("q.demo", true, "no.existe", null, null, null)))
                .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void noEliminaColasProtegidasDelSistema() {
        assertThatThrownBy(() -> service.deleteQueue("q.cmd.email"))
                .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(HttpStatus.CONFLICT));
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarColaInexistenteEs404() {
        assertThatThrownBy(() -> service.deleteQueue("q.nada"))
                .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void creaExchangeDelTipoPedido() {
        service.createExchange(new CreateExchangeRequest("ex.demo", ExchangeKind.FANOUT, false));

        ArgumentCaptor<Exchange> exchange = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(exchange.capture());
        assertThat(exchange.getValue().getType()).isEqualTo("fanout");
        assertThat(exchange.getValue().isDurable()).isFalse();
    }

    @Test
    void noEliminaExchangesProtegidos() {
        assertThatThrownBy(() -> service.deleteExchange("cmd.direct"))
                .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(HttpStatus.CONFLICT));
        verify(amqpAdmin, never()).deleteExchange(anyString());
    }

    @Test
    void bindingRequiereExchangeYColaExistentes() {
        given(management.exchangeExists("ex.demo")).willReturn(true);

        assertThatThrownBy(() -> service.createBinding(new CreateBindingRequest("ex.demo", "q.nada", "rk")))
                .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(HttpStatus.NOT_FOUND));
        verify(amqpAdmin, never()).declareBinding(any());
    }

    @Test
    void creaYEliminaBinding() {
        given(management.exchangeExists("ex.demo")).willReturn(true);
        given(amqpAdmin.getQueueInfo("q.demo")).willReturn(new QueueInformation("q.demo", 0, 0));

        service.createBinding(new CreateBindingRequest("ex.demo", "q.demo", "demo.*"));
        verify(amqpAdmin).declareBinding(any(Binding.class));

        given(management.bindings()).willReturn(List.of(new BindingDetails("ex.demo", "q.demo", "queue", "demo.*")));
        service.deleteBinding("ex.demo", "q.demo", "demo.*");
        verify(amqpAdmin).removeBinding(any(Binding.class));
    }

    @Test
    void eliminarBindingInexistenteEs404() {
        given(management.bindings()).willReturn(List.of());

        assertThatThrownBy(() -> service.deleteBinding("ex.demo", "q.demo", "x"))
                .satisfies(ex -> assertThat(statusOf(ex)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listaBindingsSinLosDelExchangePorDefecto() {
        given(management.bindings()).willReturn(List.of(
                new BindingDetails("", "q.demo", "queue", "q.demo"),
                new BindingDetails("ex.demo", "q.demo", "queue", "demo")));

        assertThat(service.listBindings()).hasSize(1);
    }

    @Test
    void publicaMensajeDePruebaConEnvelope() {
        given(management.exchangeExists("cmd.direct")).willReturn(true);

        PublishResult result = service.publish(
                new PublishMessageRequest("cmd.direct", "email.send", null, Map.of("orderId", 1)));

        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("cmd.direct"), eq("email.send"), message.capture());
        assertThat(new String(message.getValue().getBody())).contains("\"eventId\":\"" + result.eventId() + "\"")
                .contains("\"type\":\"TestMessage\"");
    }
}
