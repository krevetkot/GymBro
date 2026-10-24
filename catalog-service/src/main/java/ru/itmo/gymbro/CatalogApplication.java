package ru.itmo.gymbro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import ru.itmo.gymbro.identity.client.IdentityClient;

@SpringBootApplication
@EnableReactiveFeignClients(clients = {IdentityClient.class})
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
