package es.karmadev.locklogin.spigot.protocol;

import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.locklogin.spigot.SpigotPlugin;
import es.karmadev.locklogin.spigot.protocol.injector.ClientInjector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BungeeListener implements PluginMessageListener {

    private final SpigotPlugin plugin;

    public BungeeListener(final SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * A method that will be thrown when a PluginMessageSource sends a plugin
     * message on a registered channel.
     *
     * @param channel Channel that the message was sent through.
     * @param player  Source of the message.
     * @param message The raw message that was sent.
     */
    @Override
    public void onPluginMessageReceived(final @NotNull String channel, final @NotNull Player player, final byte @NotNull [] message) {
        if (!channel.equalsIgnoreCase("login:inject")) {
            return;
        }

        UUID target = UUID.fromString(new String(message));
        Player online = Bukkit.getPlayer(target);

        if (online == null) {
            plugin.logger().send(LogLevel.SEVERE, "Tried to inject into offline player {0}", target);
            return;
        }

        ClientInjector injector = plugin.getSpigot().getInjector();
        injector.inject(online);

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                Bukkit.getServer().sendPluginMessage(plugin, "login:inject", new byte[0]), 20);
    }
}
