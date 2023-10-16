package es.karmadev.locklogin.common.api.plugin.file.lang;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.lang.ModulePhrases;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.LanguagePackManager;
import es.karmadev.locklogin.api.plugin.file.language.Messages;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * LockLogin language pack
 */
public class InternalPack implements LanguagePackManager {

    private final ConcurrentMap<String, Path> locales = new ConcurrentHashMap<>();
    private final ConcurrentMap<Module, Map<String, Path>> moduleLocales = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CPluginMessages> messengers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Module, Map<String, ModulePhrases>> moduleMessengers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Module, String> moduleLang = new ConcurrentHashMap<>();

    private String lang;


    public InternalPack() {
        LockLogin plugin = CurrentPlugin.getPlugin();

        locales.put("en_en", plugin.workingDirectory().resolve("lang").resolve("english"));
        locales.put("english", plugin.workingDirectory().resolve("lang").resolve("english"));
        //locales.put("es_es", plugin.workingDirectory().resolve("lang").resolve("castillian"));
        //locales.put("spanish", plugin.workingDirectory().resolve("lang").resolve("castillian"));
    }

    /**
     * Get all the available languages
     *
     * @return the available languages
     */
    @Override
    public List<String> getAvailable() {
        return Collections.unmodifiableList(locales.keySet()
                .stream().sorted().collect(Collectors.toList()));
    }

    @Override
    public List<String> getAvailable(final Module module) {
        return new ArrayList<>(moduleLocales.getOrDefault(module, Collections.emptyMap()).keySet());
    }

    /**
     * Get the current language name
     *
     * @return the current language
     */
    @Override
    public String getName() {
        if (lang == null) {
            lang = "en_en";
        }

        return lang;
    }

    /**
     * Get the current module language
     * name
     *
     * @param module the module
     * @return the language name
     */
    @Override
    public String getName(final Module module) {
        return moduleLang.get(module);
    }

    /**
     * Get the current language directory
     *
     * @return the language directory
     */
    @Override
    public Path getDirectory() {
        if (locales.containsKey(lang.toLowerCase())) {
            return locales.get(lang.toLowerCase());
        }

        return locales.get("en_en");
    }

    /**
     * Get the current module language
     * directory
     *
     * @param module the module
     * @return the language directory
     */
    @Override
    public Path getDirectory(final Module module) {
        if (moduleLocales.containsKey(module)) {
            Map<String, Path> localeMap = moduleLocales.get(module);
            String locale = moduleLang.get(module);

            if (locale != null) {
                return localeMap.get(locale);
            }
        }

        return null;
    }

    /**
     * Get the pack messages
     *
     * @return the pack messages
     */
    public Messages getMessenger() {
        if (lang == null) {
            lang = "en_en";
        }

        return messengers.computeIfAbsent(lang, (messenger) -> {
            Path directory = getDirectory();

            Path file = directory.resolve("messages.yml");
            return new CPluginMessages(file, this);
        });
    }

    /**
     * Get the module messenger
     *
     * @param module the module messenger
     * @return the messenger
     */
    @Override @SuppressWarnings("unchecked")
    public <T extends ModulePhrases> T getMessenger(final Module module) {
        String lang = moduleLang.get(module);
        if (lang == null) return null;

        Map<String, ModulePhrases> phrasesMap = moduleMessengers.get(module);
        return (T) phrasesMap.get(lang);
    }

    /**
     * Register the module messenger
     *
     * @param module    the module
     * @param name      the language name
     * @param messenger the module messenger
     */
    @Override
    public <T extends ModulePhrases> void registerMessenger(final Module module, final String name, final T messenger) {
        Map<String, ModulePhrases> phrasesMap = moduleMessengers.computeIfAbsent(module, (m) -> new ConcurrentHashMap<>());
        phrasesMap.put(name, messenger);

        moduleMessengers.put(module, phrasesMap);
    }

    /**
     * Load a language
     *
     * @param name      the language name
     * @param directory the language directory
     * @return if the language was successfully loaded
     */
    @Override
    public boolean load(final String name, final Path directory) {
        if (locales.containsKey(name.toLowerCase())) return false;

        locales.put(name, directory);
        return true;
    }

    /**
     * Load a language
     *
     * @param module    the module that owns the language
     * @param name      the language name
     * @param directory the language directory
     * @return if the language pack was successfully
     * loaded
     */
    @Override
    public boolean load(final Module module, final String name, final Path directory) {
        Map<String, Path> locales = moduleLocales.computeIfAbsent(module, (m) -> new ConcurrentHashMap<>());
        if (locales.containsKey(name)) return false;

        locales.put(name, directory);
        moduleLocales.put(module, locales);
        return true;
    }

    /**
     * Unload a language
     *
     * @param name the language to unload
     */
    @Override
    public void unload(final String name) {
        locales.remove(name);
        messengers.remove(name);
        if (lang.equalsIgnoreCase(name)) {
            lang = "en_en";
        }
    }

    /**
     * Unload a language
     *
     * @param module the module that owns the language
     * @param name   the language to unload
     */
    @Override
    public void unload(final Module module, final String name) {
        Map<String, ModulePhrases> phrasesMap = moduleMessengers.computeIfAbsent(module, (m) -> new ConcurrentHashMap<>());
        Map<String, Path> locales = moduleLocales.computeIfAbsent(module, (m) -> new ConcurrentHashMap<>());

        phrasesMap.remove(name);
        locales.remove(name);

        if (phrasesMap.isEmpty() || locales.isEmpty()) {
            moduleMessengers.remove(module);
            moduleLocales.remove(module);
            moduleLang.remove(module);
        } else {
            moduleMessengers.put(module, phrasesMap);
            moduleLocales.put(module, locales);
        }
    }

    /**
     * Set the current language
     *
     * @param lang the language
     */
    @Override
    public void setLang(final String lang) {
        if (this.locales.containsKey(lang.toLowerCase())) {
            this.lang = lang;
            getMessenger();
        }
    }

    /**
     * Set the module language
     *
     * @param module the module that owns the language
     * @param name   the language name
     */
    @Override
    public void setLang(final Module module, final String name) {
        Map<String, Path> locales = moduleLocales.computeIfAbsent(module, (m) -> new ConcurrentHashMap<>());
        if (locales.containsKey(name)) {
            moduleLang.put(module, name);
        }
    }

    /**
     * Parse the message
     *
     * @param original the original message
     * @return the parsed message
     */
    String parse(final String original) {
        Configuration configuration = CurrentPlugin.getPlugin().configuration();
        String tmp = original;

        /*RGBTextComponent component = new RGBTextComponent(true, true);
        if ((original.contains("<captcha>") || original.contains("{captcha}")) && !configuration.captcha().enable())
            tmp = original.replace("<captcha>", "").replace("{captcha}", "");

        return component.parse(tmp
                .replace("{ServerName}", configuration.server())
                .replace("{newline}", "\n")
                .replace("{NewLine}", "\n"));*/

        //TODO: Use KarmaAPI2's RGB components
        return original;
    }
}
