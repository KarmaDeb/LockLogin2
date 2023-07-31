package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.file.section.SpawnSection;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.PlayerLocationStorage;
import es.karmadev.locklogin.spigot.util.storage.SpawnLocationStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnCommand implements CommandExecutor {

    private final LockLogin plugin = CurrentPlugin.getPlugin();
    private final Map<UUID, Location> previousLocations = new HashMap<>();

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Messages messages = plugin.messages();

            int id = UserDataHandler.getNetworkId(player);
            if (id > 0) {
                NetworkClient client = plugin.network().getPlayer(id);
                if (client.hasPermission(LockLoginPermission.PERMISSION_LOCATION_SPAWN)) {
                    if (args.length == 1) {
                        String arg = args[0];
                        if (arg.equalsIgnoreCase("--teleport")) {
                            Location spawnLocation = SpawnLocationStorage.load();
                            if (spawnLocation != null) {
                                Location current = player.getLocation();
                                previousLocations.put(player.getUniqueId(), current);

                                player.teleport(spawnLocation);
                                client.sendMessage(messages.prefix() + "&dRun /setloginspawn --back to go to your previous location");
                            } else {
                                client.sendMessage(messages.prefix() + "&5&oInvalid spawn location or spawn location not set");
                            }

                            return false;
                        }
                        if (arg.equalsIgnoreCase("--back")) {
                            if (previousLocations.containsKey(player.getUniqueId())) {
                                Location previous = previousLocations.remove(player.getUniqueId());
                                player.teleport(previous);
                            } else {
                                client.sendMessage(messages.prefix() + "&5&oInvalid previous location");
                            }

                            return false;
                        }
                    }

                    Location currentLocation = player.getLocation();
                    SpawnLocationStorage.assign(currentLocation);

                    client.sendMessage(messages.prefix() + messages.spawnSet());
                } else {
                    client.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_LOCATION_SPAWN));
                }
            } else {
                player.sendMessage(ColorComponent.parse(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
            }
        } else {
            switch (args.length) {
                case 1:
                    String name = args[0];
                    Player player = Bukkit.getPlayer(name);

                    if (player != null && player.isOnline()) {
                        Location location = player.getLocation();
                        SpawnLocationStorage.assign(location);

                        World world = player.getWorld();
                        double x = location.getX();
                        double y = location.getY();
                        double z = location.getZ();
                        float yaw = location.getYaw();
                        float pitch = location.getPitch();

                        plugin.info("Successfully defined spawn location at {0}{X:{1}, Y:{2}, Z:{3}} [Yaw:{4} | Pitch:{5}]", world, x, y, z, yaw, pitch);
                    } else {
                        plugin.info("Invalid player provided");
                    }
                    break;
                case 6:
                    String worldName = args[0];
                    double x, y, z;
                    float yaw, pitch;

                    try {
                        x = Double.parseDouble(args[1].replace(",", "."));
                    } catch (NumberFormatException ex) {
                        plugin.info("Invalid X coordinate provided");
                        return false;
                    }
                    try {
                        y = Double.parseDouble(args[2].replace(",", "."));
                    } catch (NumberFormatException ex) {
                        plugin.info("Invalid Y coordinate provided");
                        return false;
                    }
                    try {
                        z = Double.parseDouble(args[3].replace(",", "."));
                    } catch (NumberFormatException ex) {
                        plugin.info("Invalid Z coordinate provided");
                        return false;
                    }

                    try {
                        yaw = Float.parseFloat(args[4].replace(",", "."));
                    } catch (NumberFormatException ex) {
                        plugin.info("Invalid yaw angle provided");
                        return false;
                    }
                    try {
                        pitch = Float.parseFloat(args[5].replace(",", "."));
                    } catch (NumberFormatException ex) {
                        plugin.info("Invalid pitch angle provided");
                        return false;
                    }

                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        plugin.info("Invalid world provided");
                        return false;
                    }

                    Location location = new Location(world, x, y, z);
                    location.setYaw(yaw);
                    location.setPitch(pitch);
                    SpawnLocationStorage.assign(location);

                    plugin.info("Successfully defined spawn location at {0}{X:{1}, Y:{2}, Z:{3}} [Yaw:{4} | Pitch:{5}]", world, x, y, z, yaw, pitch);
                    break;
                default:
                    plugin.info("Please, specify a world, and the coordinates (including yaw and pitch in that order) or a player to use his location as the spawn location");
            }

        }

        return false;
    }

    private void validate(final Player player, final NetworkClient client, final UserAccount account, UserSession session, final String inputPassword) {
        HashResult hash = account.password();
        Messages messages = plugin.messages();
        Configuration configuration = plugin.configuration();

        if (hash.verify(inputPassword)) {
            session.login(true);
            session._2faLogin(true);
            session.pinLogin(true);
            client.sendMessage(messages.prefix() + messages.logged());

            if (player.hasMetadata("walkSpeed")) {
                float walkSpeed = player.getMetadata("walkSpeed").get(0).asFloat();
                player.setWalkSpeed(walkSpeed);

                player.removeMetadata("walkSpeed", (Plugin) plugin.plugin());
            }
            if (player.hasMetadata("flySpeed")) {
                float flySpeed = player.getMetadata("flySpeed").get(0).asFloat();
                player.setFlySpeed(flySpeed);

                player.removeMetadata("flySpeed", (Plugin) plugin.plugin());
            }

            SpawnSection spawn = configuration.spawn();
            if (spawn.takeBack()) {
                PlayerLocationStorage storage = new PlayerLocationStorage(client);
                Location location = storage.load();

                if (location != null) {
                    player.teleport(location);
                }
            }
        } else {
            client.sendMessage(messages.prefix() + messages.incorrectPassword());
        }
    }
}
