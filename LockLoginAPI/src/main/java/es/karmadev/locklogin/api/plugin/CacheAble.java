package es.karmadev.locklogin.api.plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a cache-able element, annotate
 * any class with this method so when the plugin
 * loads it, it will execute the static defined #preCache method
 * even before the jar gets loaded into the plugin
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheAble {

    /**
     * Get the cache object name
     *
     * @return the cache name
     */
    String name();
}
