package ru.itmo.gymbro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.itmo.gymbro.identity.client.IdentityClient;

@SpringBootApplication
@EnableFeignClients(clients = IdentityClient.class)
public class GymBroApplication {
    public static void main(String[] args) {
        SpringApplication.run(GymBroApplication.class, args);
    }
}
