package ru.itmo.gymbro.catalog.web;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.catalog.api.CatalogDirectory;
import ru.itmo.gymbro.catalog.api.GymSnapshot;
import ru.itmo.gymbro.catalog.api.SportSnapshot;

@Hidden
@RestController
@RequestMapping("/internal/v1/catalog")
class InternalCatalogController {

    private final CatalogDirectory catalog;

    InternalCatalogController(CatalogDirectory catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/sports/{id}")
    SportSnapshot getSport(@PathVariable @Positive long id) {
        return catalog.getSport(id);
    }

    @GetMapping("/gyms/{id}")
    GymSnapshot getGym(@PathVariable @Positive long id) {
        return catalog.getGym(id);
    }
}
