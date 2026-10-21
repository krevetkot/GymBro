package ru.itmo.gymbro;

import ru.itmo.gymbro.catalog.api.CatalogDirectory;
import ru.itmo.gymbro.catalog.api.GymSnapshot;
import ru.itmo.gymbro.catalog.api.SportSnapshot;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Виды спорта и залы для интеграционных тестов анкет вместо catalog-service.
 * Подменяет {@code RemoteCatalogDirectory}; сам Feign-клиент проверяется отдельно на WireMock.
 */
public class TestCatalog implements CatalogDirectory {

    private final AtomicLong ids = new AtomicLong(1_000);
    private final Map<Long, SportSnapshot> sports = new ConcurrentHashMap<>();
    private final Map<Long, GymSnapshot> gyms = new ConcurrentHashMap<>();

    public long sport() {
        long id = ids.incrementAndGet();
        sports.put(id, new SportSnapshot(id, "Спорт " + id));
        return id;
    }

    public long gym() {
        long id = ids.incrementAndGet();
        gyms.put(id, new GymSnapshot(id, "Зал " + id, "Москва", "Ленина, " + id));
        return id;
    }

    @Override
    public SportSnapshot getSport(long id) {
        SportSnapshot sport = sports.get(id);
        if (sport == null) {
            throw new NotFoundException("Вид спорта " + id + " не найден");
        }
        return sport;
    }

    @Override
    public GymSnapshot getGym(long id) {
        GymSnapshot gym = gyms.get(id);
        if (gym == null) {
            throw new NotFoundException("Зал " + id + " не найден");
        }
        return gym;
    }
}
