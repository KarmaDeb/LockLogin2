package es.karmadev.locklogin.spigot.event;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.plugin.file.lang.CPluginMessages;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;
import es.karmadev.locklogin.common.plugin.secure.CommandWhitelist;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.ui.PinInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChatHandler implements Listener {

    private final LockLoginSpigot spigot;

    public ChatHandler(final LockLoginSpigot spigot) {
        this.spigot = spigot;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        int id = UserDataHandler.getNetworkId(player);

        if (id >= 0) {
            NetworkClient client = spigot.network().getPlayer(id);
            UserSession session = client.session();

            e.setCancelled(!session.isLogged() || !session.isPinLogged() || !session.isTotpLogged());
        } else {
            if (UserDataHandler.isReady(player)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        int id = UserDataHandler.getNetworkId(player);

        if (id >= 0) {
            NetworkClient client = spigot.network().getPlayer(id);
            UserSession session = client.session();

            String message = e.getMessage();

            String command = parseCommand(message);
            String[] arguments = parseArguments(message);

            boolean pwdLogged = session.fetch("pass_logged", false);
            boolean pinLogged = session.fetch("pin_logged", false);
            boolean totLogged = session.fetch("totp_logged", false);

            if (!pwdLogged || !pinLogged || !totLogged) {
                if (CommandMask.mustMask(command)) {
                    UUID commandId = CommandMask.mask(message, arguments);

                    String masked = CommandMask.getCommand(commandId) + " " + commandId;
                    e.setMessage(masked);
                }

                Messages messages = spigot.messages();
                if (CommandWhitelist.isBlacklisted(command)) {
                    e.setCancelled(true);

                    if (!pwdLogged) {
                        client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                        return;
                    }

                    if (!pinLogged && client.account().hasPin()) {
                        if (client.account().hasPin()) {
                            client.sendMessage(messages.prefix() + messages.incorrectPin());

                            PinInventory inventory = PinInventory.getInstance(client);
                            if (inventory != null) {
                                Inventory i = inventory.getInventory();
                                player.openInventory(i);
                            }
                        } else {
                            client.sendMessage(messages.prefix() + "&cComplete the extra login steps");
                        }
                        return;
                    }

                    if (!totLogged && client.account().hasTotp()) {
                        client.sendMessage(messages.prefix() + messages.gAuthRequired());
                        return;
                    }

                    client.sendMessage(messages.prefix() + messages.completeExtra());
                }
            }
        } else {
            if (UserDataHandler.isReady(player)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsoleCommand(ServerCommandEvent e) {
        String message = e.getCommand();

        String command = parseCommand(message);
        if (command.equalsIgnoreCase("locklogin") && spigot.bungeeMode()) {
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
