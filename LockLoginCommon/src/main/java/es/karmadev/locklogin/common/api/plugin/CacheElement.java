package es.karmadev.locklogin.common.api.plugin;

import es.karmadev.api.schedule.runner.async.AsyncTaskExecutor;
import es.karmadev.locklogin.api.plugin.CacheContainer;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Provides an implementation for the cache
 * elements on the application.
 * The cache element should be thread-safe in most
 * scenarios, meaning multiple threads can read and
 * write to the element safely.
 * This might not apply in all scenarios, so proceed
 * with caution
 * @param <T> the cached element type
 */
@SuppressWarnings("unused") @ThreadSafe
public class CacheElement<T> implements CacheContainer<T> {

    private static <T> WeakReference<T> NULL_REFERENCE() {
        return new WeakReference<>(null);
    }

    private WeakReference<T> element = NULL_REFERENCE();
    private long lastModification = System.currentTimeMillis();
    private final long expiration;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public CacheElement() {
        this(null, 0, TimeUnit.MILLISECONDS);
    }

    public CacheElement(final long expiration) {
        this(null, expiration, TimeUnit.SECONDS);
    }

    public CacheElement(final long expiration, final TimeUnit unit) {
        this(null, expiration, unit);
    }

    public CacheElement(final T initialValue) {
        this(initialValue, 0, TimeUnit.MILLISECONDS);
    }

    public CacheElement(final T initialValue, final long expiration) {
        this(initialValue, expiration, TimeUnit.SECONDS);
    }

    public CacheElement(final T initialValue, final long expiration, final TimeUnit unit) {
        this.element = makeRef(initialValue);
        this.expiration = TimeUnit.MILLISECONDS.convert(Math.max(0, expiration), unit);

        AsyncTaskExecutor.EXECUTOR.scheduleAtFixedRate(() -> {
            if (this.element.get() == null) {
                this.lastModification = System.currentTimeMillis();
                return;
            }

            long diff = System.currentTimeMillis() - lastModification;
            if (diff >= this.expiration) {
                this.element = NULL_REFERENCE();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private WeakReference<T> makeRef(final T element) {
        if (element == null) return NULL_REFERENCE();
        return new WeakReference<>(element);
    }

    /**
     * Get the cached element
     *
     * @return the cached element
     */
    @Override
    public T getElement() {
        lock.readLock().lock();
        try {
            return element.get();
        } finally {
            lock.readLock().unlock();;
        }
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
        if (element.get() == null || expiration > 0 && lastModification + expiration <= System.currentTimeMillis()) {
            return other;
        }

        return getElement();
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
        if (element.get() == null || expiration > 0 && lastModification + expiration <= System.currentTimeMillis()) {
            return provider.get();
        }

        return getElement();
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
        lock.writeLock().lock();
        try {
            if (expiration > 0 && lastModification + expiration <= System.currentTimeMillis()) {
                element = NULL_REFERENCE();
            }

            if (element.get() == null) {
                element = makeRef(other);
                lastModification = System.currentTimeMillis();
            }

            return getElement();
        } finally {
            lock.writeLock().unlock();
        }
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
        lock.writeLock().lock();
        try {
            if (expiration > 0 && lastModification + expiration <= System.currentTimeMillis()) {
                element = NULL_REFERENCE();
            }
            if (element.get() == null) {
                element = makeRef(provider.get());
                lastModification = System.currentTimeMillis();
            }

            return getElement();
        } finally {
            lock.writeLock().unlock();
        }
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
        lock.writeLock().lock();
        try {
            this.element = makeRef(element);
            lastModification = System.currentTimeMillis();
        } finally {
            lock.writeLock().unlock();
        }
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
        lock.readLock().lock();

        try {
            if (this.element.get() == null) return element == null;
            return Objects.equals(this.element.get(), element);
        } finally {
            lock.readLock().unlock();
        }
    }
}
