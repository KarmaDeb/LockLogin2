package es.karmadev.locklogin.api.extension.module.lang;

/**
 * Represents the language of a
 * module. By default, all the implementations
 * of this object are protected, because
 * each implementation should provide specific
 * methods for each phrase
 */
public abstract class ModulePhrases {

    /**
     * Get a phrase
     *
     * @param path the phrase path
     * @return the phrase
     */
    protected String getString(final String path) {
        return getString(path, null);
    }

    /**
     * Get a phrase
     *
     * @param path the phrase path
     * @param def the phrase default
     * @return the phrase
     */
    protected abstract String getString(final String path, final String def);
}
