package se.magnus.microservices.composite.product;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.json.JsonMapper;

@ComponentScan("se.magnus")
@SpringBootApplication
public class ProductCompositeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
	}
	@Bean
	RestTemplate restTemplate(JsonMapper jsonMapper) {
		return new RestTemplate(List.of(new JacksonJsonHttpMessageConverter(jsonMapper)));
	}
}
