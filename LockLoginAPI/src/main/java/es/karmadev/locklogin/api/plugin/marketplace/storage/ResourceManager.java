package es.karmadev.locklogin.api.plugin.marketplace.storage;

import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.plugin.marketplace.MarketPlace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a resource manager, for
 * resources downloaded from the {@link MarketPlace marketplace}
 */
public abstract class ResourceManager {

    public final static Comparator<StoredResource> BY_ID = Comparator.comparing(StoredResource::getId);
    public final static Comparator<StoredResource> BY_NAME = Comparator.comparing(StoredResource::getName);
    public final static Comparator<StoredResource> BY_AUTHOR = Comparator.comparing(StoredResource::getPublisher);
    public final static Comparator<StoredResource> BY_CATEGORY = Comparator.comparing(StoredResource::getCategory);
    public final static Comparator<StoredResource> BY_INSTALLATION_DATE = Comparator.comparing(StoredResource::getDownloadDate);

    /**
     * Get the amount of resources
     *
     * @return the amount of resources
     */
    public abstract int getResourceCount();

    /**
     * Get all the installed resources
     *
     * @return the installed resource
     */
    public abstract Collection<? extends StoredResource> getResources();

    /**
     * Get all the (paginated) resources
     *
     * @param itemsPerPage the items per page
     * @param page the page number
     * @return the resources
     */
    public Collection<? extends StoredResource> getResources(final int itemsPerPage, final int page) {
        StoredResource[] array = getResources().toArray(new StoredResource[0]);
        return paginate(array, itemsPerPage, page);
    }

    /**
     * Get all the installed resources under
     * the specified category
     *
     * @param category the category to filter
     *                 with
     * @return the resources on the category
     */
    public Collection<? extends StoredResource> getResources(final Category category) {
        return getResources().stream()
                .filter((resource) -> resource.getCategory().equals(category) || category.equals(Category.ALL))
                .collect(Collectors.toList());
    }

    /**
     * Get all the (paginated) category resources
     *
     * @param category the resource category
     * @param itemsPerPage the items per page
     * @param page the page number
     * @return the resources
     */
    public Collection<? extends StoredResource> getResources(final Category category, final int itemsPerPage, final int page) {
        StoredResource[] array = getResources(category).toArray(new StoredResource[0]);
        return paginate(array, itemsPerPage, page);
    }

    /**
     * Uninstalls a resource
     *
     * @param resource the resource to remove
     */
    public void uninstall(final StoredResource resource) {
        uninstall(resource.getId());
    }

    /**
     * Uninstall the resource with the
     * specified ID
     *
     * @param id the resource ID
     */
    public abstract void uninstall(final int id);

    /**
     * Paginate the resource array
     *
     * @param array the array to paginate
     * @param itemsPerPage the items per page
     * @param page the page
     * @return the paginated array
     */
    private List<StoredResource> paginate(final StoredResource[] array, final int itemsPerPage, final int page) {
        if (array.length == 0) return Collections.emptyList();
        int startIndex = Math.max(itemsPerPage, 1) * Math.max(0, page);

        if (array.length <= startIndex) {
            startIndex = Math.max(0, array.length - Math.max(itemsPerPage, 1));
        }

        return new ArrayList<>(Arrays.asList(array)
                .subList(Math.max(0, startIndex - 1), Math.min(array.length, Math.max(itemsPerPage, 1))));
    }
}
