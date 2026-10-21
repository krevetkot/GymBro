package ru.itmo.gymbro.catalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.itmo.gymbro.catalog.api.GymSnapshot;
import ru.itmo.gymbro.catalog.api.SportSnapshot;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/internal/v1/catalog/sports/{id}")
    SportSnapshot getSport(@PathVariable("id") long id);

    @GetMapping("/internal/v1/catalog/gyms/{id}")
    GymSnapshot getGym(@PathVariable("id") long id);
}
