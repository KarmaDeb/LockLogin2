package es.karmadev.locklogin.api.plugin;

import java.util.function.Supplier;

/**
 * Contains cached elements for an object
 */
@SuppressWarnings("unused")
public interface CacheContainer<T> {

    /**
     * Get the cached element
     *
     * @return the cached element
     */
    T getElement();

    /**
     * Get the cached element, or
     * the default value if not present.
     * Unlike {@link #getOrElse(Object)}, this
     * method does not put the default value,
     * but uses it as an "always-safe" value
     *
     * @param other the other element
     * @return the element
     */
    T getElement(final T other);

    /**
     * Get the cached element, or
     * the default value if not present.
     * Unlike {@link #getOrElse(Supplier)}, this
     * method does not put the default value,
     * but uses it as an "always-safe" value
     *
     * @param provider the other element
     * @return the element
     */
    T getElement(final Supplier<T> provider);

    /**
     * Get the cached element or the other
     * one (and defines it) if the original
     * value is missing.
     *
     * @param other the element to use as
     *              default
     * @return the element
     */
    T getOrElse(final T other);

    /**
     * Get the cached element or supply a
     * new one by using the provided provider
     * and assigns it to the current element.
     *
     * @param provider the element provider
     * @return the element
     */
    T getOrElse(final Supplier<T> provider);

    /**
     * Refresh the cache element lifetime
     * without setting again its value
     */
    void refresh();

    /**
     * Get if the cache element can
     * expire after a defined amount of time
     * @return if the element expires
     */
    boolean expires();

    /**
     * Get if the cache element is
     * present
     *
     * @return if the element is present
     */
    default boolean isPresent() {
        return getElement((T) null) != null;
    }

    /**
     * Get the expiration time for this
     * element in milliseconds
     *
     * @return the element expiration time
     */
    long expiration();

    /**
     * Assign the element regardless if it
     * exists or not. This shouldn't be used unless
     * strictly necessary. Instead, the "safe" method
     * {@link #getOrElse(Object)} or {@link #getOrElse(Supplier)}
     * should be used, as they are meant for cases in where
     * the cache element doesn't necessary need to be always
     * available or defined (most cases). The assign method
     * could break up things and is useful for example when a
     * null default cache element can be pre-cached and can have
     * a value assignment without an external requiring its value
     *
     * @param element the new element
     */
    void assign(final T element);

    /**
     * Check if the element matches the
     * provided element
     *
     * @param element the element to check
     * @return if the element is the same
     * as the current one
     * @param <A> the type of the element to check
     * @param <B> the matching type of the element
     */
    <A extends T, B extends A> boolean elementEquals(final B element);
}
