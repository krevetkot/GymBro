package ru.itmo.gymbro.shared.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * Reactor поверх Spring Data JPA. JPA блокирует поток на время запроса к базе, а потоков event loop
 * у WebFlux мало, поэтому работа с репозиториями уходит на {@link Schedulers#boundedElastic()} —
 * пул для блокирующих задач. Транзакция открывается там же через {@link TransactionTemplate}:
 * {@code @Transactional} на методах, возвращающих {@code Mono}, искал бы реактивный менеджер
 * транзакций, а у JPA его нет.
 */
@Component
public class BlockingJpa {

    private final TransactionTemplate readOnly;
    private final TransactionTemplate readWrite;

    public BlockingJpa(PlatformTransactionManager transactionManager) {
        this.readWrite = new TransactionTemplate(transactionManager);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    /** Чтение в read-only транзакции; {@code null} от работы — пустой {@code Mono}. */
    public <T> Mono<T> read(Supplier<T> work) {
        return run(readOnly, work);
    }

    /** Изменение в транзакции: исключение внутри работы откатывает её целиком. */
    public <T> Mono<T> write(Supplier<T> work) {
        return run(readWrite, work);
    }

    private static <T> Mono<T> run(TransactionTemplate transaction, Supplier<T> work) {
        return Mono.fromCallable(() -> transaction.execute(status -> work.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
