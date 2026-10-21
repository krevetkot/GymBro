package ru.itmo.gymbro.catalog.client;

import org.springframework.stereotype.Component;
import ru.itmo.gymbro.catalog.api.CatalogDirectory;
import ru.itmo.gymbro.catalog.api.GymSnapshot;
import ru.itmo.gymbro.catalog.api.SportSnapshot;

import static ru.itmo.gymbro.shared.client.RemoteCalls.call;

@Component
class RemoteCatalogDirectory implements CatalogDirectory {

    private static final String SERVICE = "каталога";

    private final CatalogClient client;

    RemoteCatalogDirectory(CatalogClient client) {
        this.client = client;
    }

    @Override
    public SportSnapshot getSport(long id) {
        return call(SERVICE, "Вид спорта " + id + " не найден", () -> client.getSport(id));
    }

    @Override
    public GymSnapshot getGym(long id) {
        return call(SERVICE, "Зал " + id + " не найден", () -> client.getGym(id));
    }
}
