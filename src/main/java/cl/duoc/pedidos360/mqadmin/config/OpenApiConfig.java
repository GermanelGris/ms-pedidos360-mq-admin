package cl.duoc.pedidos360.mqadmin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI mqAdminOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Pedidos360 - Administrador de RabbitMQ")
                .description("Crea, consulta y elimina colas, exchanges y bindings. Uso interno: se expone a través "
                        + "del BFF en /api/mq/** solo para el rol Admin.")
                .version("v1"));
    }
}
