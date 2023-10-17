package es.karmadev.locklogin.spigot.protocol;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.spigot.protocol.protocol.chat.ChatManager;
import es.karmadev.locklogin.spigot.protocol.protocol.premium.ProtocolListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ProtocolAssistant {

    public static boolean isProtocolSupported() {
        return Bukkit.getServer().getPluginManager().isPluginEnabled("ProtocolLib");
    }

    public static void registerListener() {
        LockLogin plugin = CurrentPlugin.getPlugin();

        if (isProtocolSupported() && !plugin.bungeeMode()) {
            plugin.logInfo("Detected ProtocolLib, hooking into it");
            ProtocolListener.register();
        }
    }

    public static void clearChat(final Player player) {
        if (isProtocolSupported()) {
            ChatManager.clearProtocolChat(player);
            return;
        }

        for (int i = 0; i < 200; i++) player.sendMessage("");
    }
}
