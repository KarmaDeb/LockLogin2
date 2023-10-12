package es.karmadev.locklogin.api.task;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiConsumer;

public interface BiTask<T, B> extends BiConsumer<T, B> {

    /**
     * Throw an exception on the task running
     * thread
     *
     * @param exception the exception to throw
     */
    default void throwException(final Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * Returns a composed {@code BiConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code BiConsumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    @NotNull
    @Override
    default BiTask<T, B> andThen(@NotNull BiConsumer<? super T, ? super B> after) {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }
}
