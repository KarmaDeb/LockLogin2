package es.karmadev.locklogin.api.plugin.runtime.dependency.shade;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a set of relocations
 */
public interface RelocationSet {

    /**
     * Get if the dependency has
     * relocations
     *
     * @return if the dependency has relocations
     */
    boolean hasRelocation();

    /**
     * Iterate to the next relocation
     *
     * @return the next relocation
     */
    @Nullable
    Relocation next();
}
