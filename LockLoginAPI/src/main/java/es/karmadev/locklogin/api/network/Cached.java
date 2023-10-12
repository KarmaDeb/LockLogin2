package es.karmadev.locklogin.api.network;

/**
 * Represents an object that contains cached elements ({@link es.karmadev.locklogin.api.plugin.CacheContainer}).
 * Implementing an object with this interface will allow any external
 * implementation to run the {@link #reset()} method, which should (depends on
 * implementation) clear all caches, and also allow {@link #reset(String)} which
 * should reset all the caches known by that name on the implementation
 */
@FunctionalInterface
public interface Cached {

    /**
     * Reset all the caches
     */
    default void reset() {
        reset(null);
    }

    /**
     * Reset the cache, implementations
     * should interpreter null as "everything"
     *
     * @param name the cache name to reset
     */
    void reset(final String name);
}
