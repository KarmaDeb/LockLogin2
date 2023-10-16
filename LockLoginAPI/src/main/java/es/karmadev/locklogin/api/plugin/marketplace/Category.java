package es.karmadev.locklogin.api.plugin.marketplace;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a known {@link MarketPlace marketplace} category
 */
@AllArgsConstructor @Getter
public enum Category {
    ALL(10),
    TRANSLATION(11),
    MODULE(12),
    EMAIL_TEMPLATE(-1); //-1 categories are the ones that don't yet exist

    private final int id;

    /**
     * Get a category by its ID
     *
     * @param id the category id
     * @return the category
     */
    public static Category byId(final int id) {
        for (Category category : Category.values()) {
            if (category.id == id) return category;
        }

        return null;
    }

    /**
     * Get the category pretty name
     *
     * @return the category pretty name
     */
    public String prettyName() {
        return name().toLowerCase().replace("_", " ");
    }
}
