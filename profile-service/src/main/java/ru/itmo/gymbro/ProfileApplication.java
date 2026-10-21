package ru.itmo.gymbro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.itmo.gymbro.catalog.client.CatalogClient;
import ru.itmo.gymbro.identity.client.IdentityClient;

@SpringBootApplication
@EnableFeignClients(clients = {IdentityClient.class, CatalogClient.class})
public class ProfileApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileApplication.class, args);
    }
}
