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
    MODULE(12);

    private final int id;
}
