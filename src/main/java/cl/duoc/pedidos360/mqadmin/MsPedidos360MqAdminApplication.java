package cl.duoc.pedidos360.mqadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsPedidos360MqAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsPedidos360MqAdminApplication.class, args);
	}

}
