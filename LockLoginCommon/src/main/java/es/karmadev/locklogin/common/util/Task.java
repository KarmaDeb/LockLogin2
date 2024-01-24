package es.karmadev.locklogin.common.util;

import java.util.function.Consumer;

/**
 * LockLogin task
 */
public interface Task<T> {

    /**
     * Apply the object
     *
     * @param object the object
     */
    void apply(final T object);

    /**
     * Execute an action over
     * the object when complete
     *
     * @param object the object
     */
    void then(final Consumer<T> object);
}
