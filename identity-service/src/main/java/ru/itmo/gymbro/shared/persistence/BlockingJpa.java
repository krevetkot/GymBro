package ru.itmo.gymbro.shared.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

@Component
public class BlockingJpa {

    private final TransactionTemplate readOnly;
    private final TransactionTemplate readWrite;

    public BlockingJpa(PlatformTransactionManager transactionManager) {
        this.readWrite = new TransactionTemplate(transactionManager);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    public <T> Mono<T> read(Supplier<T> work) {
        return run(readOnly, work);
    }

    public <T> Mono<T> write(Supplier<T> work) {
        return run(readWrite, work);
    }

    private static <T> Mono<T> run(TransactionTemplate transaction, Supplier<T> work) {
        return Mono.fromCallable(() -> transaction.execute(status -> work.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
