package ru.itmo.gymbro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.itmo.gymbro.identity.client.IdentityClient;
import ru.itmo.gymbro.profile.client.ProfileClient;

@SpringBootApplication
@EnableFeignClients(clients = {IdentityClient.class, ProfileClient.class})
public class MatchingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchingApplication.class, args);
    }
}
