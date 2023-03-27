package es.karmadev.locklogin.spigot.event;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;
import es.karmadev.locklogin.common.plugin.secure.CommandWhitelist;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChatHandler implements Listener {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        int id = UserDataHandler.getNetworkId(player);
        if (id >= 0) {
            NetworkClient client = plugin.network().getPlayer(id);
            UserSession session = client.session();

            e.setCancelled(!session.isLogged() || !session.isPinLogged() || !session.is2FALogged());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        int id = UserDataHandler.getNetworkId(player);

        if (id >= 0) {
            NetworkClient client = plugin.network().getPlayer(id);
            UserSession session = client.session();

            String message = e.getMessage();

            String command = parseCommand(message);
            String[] arguments = parseArguments(message);

            if (!session.isLogged() || !session.isPinLogged() || !session.is2FALogged()) {
                if (CommandMask.mustMask(command)) {
                    UUID commandId = CommandMask.mask(message, arguments);
                    e.setMessage(CommandMask.getCommand(commandId) + " " + commandId);
                }

                Messages messages = plugin.messages();
                if (CommandWhitelist.isBlacklisted(command)) {
                    if (session.isLogged()) {
                        client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                    } else {
                        if (session.isPinLogged()) {
                            client.sendMessage(messages.prefix() + messages.gAuthRequired());
                        } else {
                            client.sendMessage(messages.prefix() + messages.incorrectPin());
                            //TODO: Open pin inventory
                        }
                    }

                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsoleCommand(ServerCommandEvent e) {
        String message = e.getCommand();

        String command = parseCommand(message);
        if (command.equalsIgnoreCase("locklogin") && plugin.bungeeMode()) {
            e.setCommand(message.replaceFirst("locklogin", "spigot-locklogin"));
        }
    }

    private String parseCommand(final String command) {
        String parseTarget = command;
        if (command.contains(" ")) {
            String[] data = command.split(" ");
            parseTarget = data[0];
        }

        if (parseTarget.contains(":")) {
            String[] data = parseTarget.split(":");
            String plugin = data[0];
            parseTarget = parseTarget.replaceFirst(plugin + ":", "");
        }

        return parseTarget;
    }

    private String[] parseArguments(final String command) {
        List<String> arguments = new ArrayList<>();
        if (command.contains(" ")) {
            String[] data = command.split(" ");
            if (data.length >= 1) {
                arguments.addAll(Arrays.asList(Arrays.copyOfRange(data, 1, data.length)));
            }
        }

        return arguments.toArray(new String[0]);
    }
}
