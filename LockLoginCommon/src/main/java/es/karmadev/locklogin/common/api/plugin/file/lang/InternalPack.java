package es.karmadev.locklogin.common.api.plugin.file.lang;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LockLogin language pack
 */
public class InternalPack {

    public String packFileName() {
        Configuration configuration = CurrentPlugin.getPlugin().configuration();
        String lang = configuration.language();

        String name = "messages.yml";
        switch (lang.toLowerCase().replace("_", " ")) {
            case "en_en":
            case "english":
                name = "messages_en.yml";
                break;
            case "es_es":
            case "spanish":
                name = "messages_es.yml";
                break;
        }

        return name;
    }

    public String packDirectoryName() {
        Configuration configuration = CurrentPlugin.getPlugin().configuration();
        String lang = configuration.language();

        String dir = lang.replaceAll("\\s", "_");
        switch (lang.toLowerCase().replace("_", " ")) {
            case "en_en":
            case "english":
                dir = "english";
                break;
            case "es_es":
            case "spanish":
                dir = "castillian";
                break;
        }

        return dir;
    }

    /**
     * Get the pack messages
     *
     * @return the pack messages
     */
    public Messages getMessenger() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        String lang = configuration.language();

        String name = "messages.yml";
        String dir = lang.replaceAll("\\s", "_");
        switch (lang.toLowerCase().replace("_", " ")) {
            case "en_en":
            case "english":
                name = "messages_en.yml";
                dir = "english";
                break;
            case "es_es":
            case "spanish":
                name = "messages_es.yml";
                dir = "castillian";
                break;
        }

        Path file = plugin.workingDirectory().resolve("lang").resolve(dir).resolve("messages.yml");
        boolean generate = false;
        try (InputStream internal_file = plugin.load("plugin/yaml/" + name)) {
            if (internal_file != null) {
                generate = true;
            }
        } catch (IOException ignored) {}

        if (generate && !Files.exists(file)) {
            PathUtilities.copy(CurrentPlugin.getPlugin(), "plugin/yaml/" + name, file);
        }

        return new CPluginMessages(file, this);
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
