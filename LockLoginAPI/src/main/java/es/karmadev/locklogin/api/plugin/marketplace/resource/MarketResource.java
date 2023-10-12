package es.karmadev.locklogin.api.plugin.marketplace.resource;

import es.karmadev.locklogin.api.plugin.marketplace.MarketPlace;
import es.karmadev.locklogin.api.plugin.marketplace.Category;

/**
 * {@link MarketPlace Marketplace} resource
 */
public interface MarketResource {

    /**
     * Get the resource ID
     *
     * @return the resource ID
     */
    int getId();

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
     * Get the amount of downloads the
     * resource has
     *
     * @return the resource download amount
     */
    int getDownloads();

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
     * Get the resource download
     * information
     */
    ResourceDownload getDownload();
}
