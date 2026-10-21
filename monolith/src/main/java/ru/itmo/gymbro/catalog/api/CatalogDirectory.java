package ru.itmo.gymbro.catalog.api;

/** Read-only contract for other domains; never exposes persistence entities. */
public interface CatalogDirectory {

    SportSnapshot getSport(long id);

    GymSnapshot getGym(long id);
}
