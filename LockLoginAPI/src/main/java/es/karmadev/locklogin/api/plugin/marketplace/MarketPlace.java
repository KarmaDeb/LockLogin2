package es.karmadev.locklogin.api.plugin.marketplace;

import es.karmadev.api.schedule.task.completable.late.LateTask;
import es.karmadev.locklogin.api.plugin.marketplace.resource.MarketResource;
import es.karmadev.locklogin.api.plugin.marketplace.storage.ResourceManager;
import es.karmadev.locklogin.api.task.FutureTask;

import java.util.Collection;

/**
 * The LockLogin marketplace is basically
 * the resources posted on the <a href="https://forum.karmadev.es">forum</a>
 * under the categories of LockLogin
 */
public interface MarketPlace {

    /**
     * Get the marketplace version
     *
     * @return the marketplace version
     */
    int getVersion();

    /**
     * Get the amount of pages on the
     * specified resource category
     *
     * @param category the category
     * @return the amount of pages for the category
     */
    FutureTask<Integer> getPages(final Category category);

    /**
     * Get the amount of resources on
     * that category
     *
     * @param category the category
     * @return the resources under the category
     */
    FutureTask<Integer> getResourcesAmount(final Category category);

    /**
     * Get the amount of resources on
     * that category and the specified
     * page
     *
     * @param category the category
     * @param page the page
     * @return the resources under the category and
     * the page
     */
    FutureTask<Integer> getResourcesAmount(final Category category, final int page);

    /**
     * Get all the resources for
     * the category on the specified
     * page
     *
     * @param category the category
     * @return the resources on the page
     */
    FutureTask<Collection<? extends MarketResource>> getResources(final Category category, final int page);

    /**
     * Get a resource
     *
     * @param id the resource id
     * @return the resource
     */
    FutureTask<MarketResource> getResource(final int id);

    /**
     * Get the resource manager
     *
     * @return the resource manager
     */
    ResourceManager getManager();
}
