package ru.itmo.gymbro.catalog.api;

public interface CatalogDirectory {

    SportSnapshot getSport(long id);

    GymSnapshot getGym(long id);
}
