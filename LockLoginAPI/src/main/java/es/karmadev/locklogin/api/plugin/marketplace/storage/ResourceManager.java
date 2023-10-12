package es.karmadev.locklogin.api.plugin.marketplace.storage;

import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.plugin.marketplace.MarketPlace;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Represents a resource manager, for
 * resources downloaded from the {@link MarketPlace marketplace}
 */
public interface ResourceManager {

    /**
     * Get all the installed resources
     *
     * @return the installed resource
     */
    Collection<? extends StoredResource> getResources();

    /**
     * Get all the installed resources under
     * the specified category
     *
     * @param category the category to filter
     *                 with
     * @return the resources on the category
     */
    default Collection<? extends StoredResource> getResources(final Category category) {
        return getResources().stream()
                .filter((resource) -> resource.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    /**
     * Uninstalls a resource
     *
     * @param resource the resource to remove
     */
    default void uninstall(final StoredResource resource) {
        uninstall(resource.getId());
    }

    /**
     * Uninstall the resource with the
     * specified ID
     *
     * @param id the resource ID
     */
    void uninstall(final int id);
}
