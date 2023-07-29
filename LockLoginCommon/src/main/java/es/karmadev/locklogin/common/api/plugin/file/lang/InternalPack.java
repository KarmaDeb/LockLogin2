package es.karmadev.locklogin.common.api.plugin.file.lang;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * LockLogin language pack
 */
public class InternalPack {

    /**
     * Get the pack messages
     *
     * @return the pack messages
     */
    public Messages getMessenger() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = CurrentPlugin.getPlugin().configuration();
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
            case "fr_fr":
            case "french":
                name = "messages_fr.yml";
                dir = "french";
                break;
            case "zh_cn":
            case "chinese simplified":
            case "simplified chinese":
                name = "messages_zh.yml";
                dir = "simplified_chinese";
                break;
            case "pl_pl":
            case "polish":
                name = "messages_pl.yml";
                dir = "polish";
                break;
            case "tr_tr":
            case "turkish":
                name = "messages_tr.yml";
                dir = "turkish";
                break;
        }

        Path file = plugin.workingDirectory().resolve("lang").resolve(dir).resolve("messages.yml");
        boolean generate = false;
        try (InputStream internal_file = plugin.load("plugin/yaml/" + name)) {
            if (internal_file != null) {
                generate = true;
            }
        } catch (IOException ignored) {}

        if (generate) {
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
