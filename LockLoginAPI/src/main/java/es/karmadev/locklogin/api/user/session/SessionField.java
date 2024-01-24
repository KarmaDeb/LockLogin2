package es.karmadev.locklogin.api.user.session;

import java.lang.reflect.Type;

/**
 * Session value
 *
 * @param <T> the value type
 */
public interface SessionField<T> {

    /**
     * Get the value type
     *
     * @return the type
     */
    Type type();

    /**
     * Get the session key
     *
     * @return the sesion key
     */
    String key();

    /**
     * Get the value
     *
     * @return the value
     */
    T get();
}
