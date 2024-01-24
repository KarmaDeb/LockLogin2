package es.karmadev.locklogin.api.plugin.file.language;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.lang.ModulePhrases;

import java.nio.file.Path;
import java.util.List;

/**
 * LockLogin language pack manager
 */
public interface LanguagePackManager {

    /**
     * Get all the available languages
     *
     * @return the available languages
     */
    List<String> getAvailable();

    List<String> getAvailable(final Module module);

    /**
     * Get the current language name
     *
     * @return the current language
     */
    String getName();

    /**
     * Get the current module language
     * name
     *
     * @param module the module
     * @return the language name
     */
    String getName(final Module module);

    /**
     * Get the current language directory
     *
     * @return the language directory
     */
    Path getDirectory();

    /**
     * Get the current module language
     * directory
     *
     * @param module the module
     * @return the language directory
     */
    Path getDirectory(final Module module);

    /**
     * Get the language messages
     *
     * @return the language messages
     */
    Messages getMessenger();

    /**
     * Get the module messenger
     *
     * @param module the module messenger
     * @return the messenger
     * @param <T> the messenger object type
     */
    <T extends ModulePhrases> T getMessenger(final Module module);

    /**
     * Register the module messenger
     *
     * @param module the module
     * @param name the language name
     * @param messenger the module messenger
     * @param <T> the messenger type
     */
    <T extends ModulePhrases> void registerMessenger(final Module module, final String name, final T messenger);

    /**
     * Load a language
     *
     * @param name      the language name
     * @param directory the language directory
     * @return if the language pack was successfully
     * loaded
     */
    boolean load(final String name, final Path directory);

    /**
     * Load a language
     *
     * @param module the module that owns the language
     * @param name   the language name
     * @param directory the language directory
     * @return if the language pack was successfully
     * loaded
     */
    boolean load(final Module module, final String name, final Path directory);

    /**
     * Unload a language
     *
     * @param name the language to unload
     */
    void unload(final String name);

    /**
     * Unload a language
     *
     * @param module the module that owns the language
     * @param name the language to unload
     */
    void unload(final Module module, final String name);

    /**
     * Set the current language
     *
     * @param lang the language
     */
    void setLang(final String lang);

    /**
     * Set the module language
     *
     * @param module the module that owns the language
     * @param name the language name
     */
    void setLang(final Module module, final String name);
}
