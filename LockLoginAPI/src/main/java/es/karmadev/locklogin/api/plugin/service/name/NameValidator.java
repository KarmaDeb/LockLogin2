package es.karmadev.locklogin.api.plugin.service.name;

import es.karmadev.locklogin.api.plugin.service.PluginService;

/**
 * LockLogin name validator service
 */
public interface NameValidator extends PluginService {

    /**
     * Validate the name
     *
     * @param name the name to validate
     */
    void validate(final String name);

    /**
     * Get if the name is valid
     *
     * @return if the name is valid
     */
    boolean isValid();

    /**
     * Get the name invalid characters
     *
     * @return the invalid characters
     */
    String invalidCharacters();
}
