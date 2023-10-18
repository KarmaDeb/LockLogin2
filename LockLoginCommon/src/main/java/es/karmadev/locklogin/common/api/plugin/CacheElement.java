package es.karmadev.locklogin.common.api.plugin;

import es.karmadev.locklogin.api.plugin.CacheContainer;

import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class CacheElement<T> implements CacheContainer<T> {

    private T element;
    private long lastModification = System.currentTimeMillis();
    private final long expiration;

    public CacheElement() {
        this(null, 0);
    }

    public CacheElement(final long expiration) {
        this(null, expiration);
    }

    public CacheElement(final T initialValue) {
        this(initialValue, 0);
    }

    public CacheElement(final T initialValue, final long expiration) {
        this.element = initialValue;
        this.expiration = expiration;
    }

    /**
     * Get the cached element
     *
     * @return the cached element
     */
    @Override
    public T getElement() {
        return element;
    }

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
    @Override
    public T getElement(final T other) {
        if (element == null || expiration > 0 && lastModification + expiration <= System.currentTimeMillis()) {
            return other;
        }

        return element;
    }

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
    @Override
    public T getElement(final Supplier<T> provider) {
        if (element == null || expiration > 0 && lastModification + expiration <= System.currentTimeMillis()) {
            return provider.get();
        }

        return element;
    }

    /**
     * Get the cached element or the other
     * one (and defines it) if the original
     * value is missing.
     *
     * @param other the element to use as
     *              default
     * @return the element
     */
    @Override
    public T getOrElse(final T other) {
        if (expiration > 0 && lastModification + expiration <= System.currentTimeMillis()) {
            element = null;
        }
        if (element == null) {
            element = other;
            lastModification = System.currentTimeMillis();
        }

        return element;
    }

    /**
     * Get the cached element or supply a
     * new one by using the provided provider
     * and assigns it to the current element.
     *
     * @param provider the element provider
     * @return the element
     */
    @Override
    public T getOrElse(final Supplier<T> provider) {
        if (expiration > 0 && lastModification + expiration <= System.currentTimeMillis()) {
            element = null;
        }
        if (element == null) {
            element = provider.get();
            lastModification = System.currentTimeMillis();
        }

        return element;
    }

    /**
     * Refresh the cache element lifetime
     * without setting again its value
     */
    @Override
    public void refresh() {
        lastModification = System.currentTimeMillis();
    }

    /**
     * Get if the cache element can
     * expire after a defined amount of time
     *
     * @return if the element expires
     */
    @Override
    public boolean expires() {
        return expiration > 0;
    }

    /**
     * Get the expiration time for this
     * element in milliseconds
     *
     * @return the element expiration time
     */
    @Override
    public long expiration() {
        return expiration;
    }

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
    @Override
    public void assign(final T element) {
        this.element = element;
        lastModification = System.currentTimeMillis();
    }

    /**
     * Check if the element matches the
     * provided element
     *
     * @param element the element to check
     * @return if the element is the same
     * as the current one
     */
    @Override
    public <A extends T, B extends A> boolean elementEquals(final B element) {
        if (this.element == null) return element == null;
        return Objects.equals(this.element, element);
    }
}
