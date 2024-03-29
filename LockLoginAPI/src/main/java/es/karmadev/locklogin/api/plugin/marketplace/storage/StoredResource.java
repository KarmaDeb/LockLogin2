package es.karmadev.locklogin.api.plugin.marketplace.storage;

import es.karmadev.locklogin.api.plugin.marketplace.Category;

import java.time.Instant;

/**
 * Represents a stored resource, unlike
 * {@link es.karmadev.locklogin.api.plugin.marketplace.resource.MarketResource}, this object
 * is cached-version of the downloaded resource,
 * meaning it will be always have the same data
 */
public interface StoredResource {

    /**
     * Get the resource ID
     *
     * @return the resource ID
     */
    int getId();

    /**
     * Get if the resource is loaded
     *
     * @return if the resource is loaded
     */
    boolean isLoaded();

    /**
     * Loads the resource, for translations, this
     * will make the plugin to switch to the translation,
     * meanwhile for modules, will make the module
     * to load
     */
    void load();

    /**
     * Unloads the resource, for translations, this
     * will make the plugin switch to the default
     * language, meanwhile for modules, will make
     * the module to unload
     */
    void unload();

    /**
     * Get the resource category
     *
     * @return the category
     */
    Category getCategory();

    /**
     * Get the resource name
     *
     * @return the resource name
     */
    String getName();

    /**
     * Get the resource description
     *
     * @return the resource description
     */
    String getDescription();

    /**
     * Get the name of the publisher
     * for the specified resource
     *
     * @return the publisher
     */
    String getPublisher();

    /**
     * Get the resource version
     *
     * @return the resource version
     */
    String getVersion();

    /**
     * Get when the resource was
     * downloaded
     *
     * @return the resource download date
     */
    Instant getDownloadDate();
}
