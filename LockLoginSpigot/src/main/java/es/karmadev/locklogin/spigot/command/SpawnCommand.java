package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.file.spawn.SpawnConfiguration;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.SpawnLocationStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@PluginCommand(command = "spawn", useInBungeecord = true)
@SuppressWarnings("unused")
public class SpawnCommand extends Command {

    private final LockLogin plugin = CurrentPlugin.getPlugin();
    private final Map<UUID, Location> previousLocations = new HashMap<>();

    public SpawnCommand(final String cmd) {
        super(cmd);
    }

    /**
     * Executes the command, returning its success
     *
     * @param sender       Source object which is executing this command
     * @param label        The alias of the command used
     * @param args         All arguments passed to the command, split via ' '
     * @return true if the command was successful, otherwise false
     */
    @Override
    public boolean execute(final @NotNull CommandSender sender, final @NotNull String label, @NotNull String[] args) {
        Messages messages = plugin.messages();

        if (sender instanceof Player) {
            Player player = (Player) sender;

            int id = UserDataHandler.getNetworkId(player);
            if (id > 0) {
                NetworkClient client = plugin.network().getPlayer(id);
                if (client.hasPermission(LockLoginPermission.PERMISSION_LOCATION_SPAWN)) {
                    if (args.length == 0) {
                        SpawnConfiguration config = plugin.configuration().spawn();
                        if (config.teleport()) {
                            if (client.hasPermission(LockLoginPermission.PERMISSION_LOCATION_SPAWN)) {
                                if (UserDataHandler.isTeleporting(player)) {
                                    UserDataHandler.callTeleportSignal(player);
                                    return false;
                                }

                                Location spawn = SpawnLocationStorage.load();
                                Location current = player.getLocation();

                                if (spawn == null) {
                                    client.sendMessage(messages.prefix() + messages.spawnNotSet());
                                    return false;
                                }

                                if (current.distance(spawn) <= config.spawnRadius()) {
                                    client.sendMessage(messages.prefix() + messages.spawnTeleportNear());
                                    return false;
                                }

                                AtomicInteger delay = new AtomicInteger(config.teleportDelay());
                                if (delay.get() > 0) {
                                    UserDataHandler.setTeleporting(player, () ->
                                            client.sendMessage(messages.prefix() + messages.spawnTeleportTime(delay.get())));

                                    client.sendMessage(messages.prefix() + messages.spawnTeleportTime(delay.get()));
                                    BukkitRunnable runnable = new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            if (!UserDataHandler.isTeleporting(player)) {
                                                cancel();

                                                //We've got cancelled
                                                return;
                                            }

                                            int remaining = delay.getAndDecrement();
                                            if (remaining % 5 == 0 && remaining < config.teleportDelay() && remaining > 0) {
                                                client.sendMessage(messages.prefix() + messages.spawnTeleportTime(remaining));
                                                return;
                                            }

                                            if (remaining <= 0) {
                                                Bukkit.getScheduler().runTask((Plugin) plugin.plugin(), () -> player.teleport(spawn, PlayerTeleportEvent.TeleportCause.COMMAND));
                                                client.sendMessage(messages.prefix() + messages.spawnTeleport());
                                                cancel();
                                                UserDataHandler.setTeleporting(player, null);
                                            }
                                        }
                                    };

                                    runnable.runTaskTimerAsynchronously((Plugin) plugin.plugin(), 0, 20);
                                    return false;
                                }

                                player.teleport(spawn, PlayerTeleportEvent.TeleportCause.COMMAND);
                                client.sendMessage(messages.prefix() + messages.spawnTeleport());
                            } else {
                                client.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_LOCATION_SPAWN));
                            }

                            return false;
                        }

                        client.sendMessage(messages.prefix() + messages.spawnUsage());
                        return false;
                    }

                    String argument = args[0].toLowerCase();
                    switch (argument) {
                        case "set":
                            if (client.hasPermission(LockLoginPermission.PERMISSION_LOCATION_SPAWN_SET)) {
                                Location location = player.getLocation();
                                SpawnLocationStorage.assign(location);

                                client.sendMessage(messages.prefix() + messages.spawnSet(locationString(location)));
                            } else {
                                client.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_LOCATION_SPAWN_SET));
                            }
                            break;
                        case "unset":
                            if (client.hasPermission(LockLoginPermission.PERMISSION_LOCATION_SPAWN_SET)) {
                                Location spawn = SpawnLocationStorage.load();
                                if (spawn == null) {
                                    client.sendMessage(messages.prefix() + messages.spawnNotSet());
                                    return false;
                                }

                                SpawnLocationStorage.assign(null);
                                client.sendMessage(messages.prefix() + messages.spawnUnset());
                            } else {
                                client.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_LOCATION_SPAWN_SET));
                            }
                            break;
                        case "teleport":
                            if (client.hasPermission(LockLoginPermission.PERMISSION_LOCATION_SPAWN_TELEPORT)) {
                                Location spawn = SpawnLocationStorage.load();
                                if (spawn == null) {
                                    client.sendMessage(messages.prefix() + messages.spawnNotSet());
                                    return false;
                                }

                                Location current = player.getLocation();
                                if (spawn.distance(current) <= plugin.configuration().spawn().spawnRadius()) {
                                    client.sendMessage(messages.prefix() + messages.spawnTeleportNear());
                                    return false;
                                }

                                previousLocations.put(player.getUniqueId(), current);
                                player.teleport(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
                                /*
                                As this is an administrator case, we teleport
                                the client as if we wanted him to go the location.
                                Hopefully, by doing this other plugins don't break
                                us
                                 */

                                client.sendMessage(messages.prefix() + messages.spawnTeleportAdmin());
                            } else {
                                client.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_LOCATION_SPAWN_TELEPORT));
                            }
                            break;
                        case "back":
                            if (client.hasPermission(LockLoginPermission.PERMISSION_LOCATION_SPAWN_TELEPORT)) {
                                Location previous = previousLocations.remove(player.getUniqueId());
                                if (previous == null) {
                                    client.sendMessage(messages.prefix() + messages.spawnNoBack());
                                    return false;
                                }

                                player.teleport(previous, PlayerTeleportEvent.TeleportCause.PLUGIN);
                                client.sendMessage(messages.prefix() + messages.spawnBack());
                            } else {
                                client.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_LOCATION_SPAWN_TELEPORT));
                            }
                            break;
                    }
                } else {
                    client.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_LOCATION_SPAWN));
                }
            } else {
                player.sendMessage(ColorComponent.parse(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
            }
        } else {
            if (args.length == 0) {
                plugin.info(messages.spawnUsage());
                return false;
            }

            String argument = args[0].toLowerCase();
            switch (argument) {
                case "set":
                    switch (args.length) {
                        case 2:
                            String name = args[1];
                            Player player = Bukkit.getPlayer(name);

                            if (player != null && player.isOnline()) {
                                Location location = player.getLocation();
                                SpawnLocationStorage.assign(location);

                                plugin.info(messages.spawnSet(locationString(location)));
                            } else {
                                plugin.info("Invalid player provided");
                            }
                            break;
                        case 7:
                            String worldName = args[1];
                            double x, y, z;
                            float yaw, pitch;

                            try {
                                x = Double.parseDouble(args[2].replace(",", "."));
                            } catch (NumberFormatException ex) {
                                plugin.info("Invalid X coordinate provided");
                                return false;
                            }
                            try {
                                y = Double.parseDouble(args[3].replace(",", "."));
                            } catch (NumberFormatException ex) {
                                plugin.info("Invalid Y coordinate provided");
                                return false;
                            }
                            try {
                                z = Double.parseDouble(args[4].replace(",", "."));
                            } catch (NumberFormatException ex) {
                                plugin.info("Invalid Z coordinate provided");
                                return false;
                            }

                            try {
                                yaw = Float.parseFloat(args[5].replace(",", "."));
                            } catch (NumberFormatException ex) {
                                plugin.info("Invalid yaw angle provided");
                                return false;
                            }
                            try {
                                pitch = Float.parseFloat(args[6].replace(",", "."));
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

                            plugin.info(messages.spawnSet(locationString(location)));
                            break;
                        default:
                            plugin.info("Please, specify a world, and the coordinates (including yaw and pitch in that order) or a player to use his location as the spawn location");
                            break;
                    }
                case "unset":
                    Location spawn = SpawnLocationStorage.load();
                    if (spawn == null) {
                        plugin.info(messages.spawnNotSet());
                        return false;
                    }

                    SpawnLocationStorage.assign(null);
                    plugin.info(messages.spawnUnset());
                    break;
                case "teleport":
                case "back":
                default:
                    plugin.warn("This command can be run only by players");
                    break;
            }
        }

        return false;
    }

    private String locationString(final Location location) {
        World world = location.getWorld();
        String worldName = "undefined";
        if (world != null) {
            worldName = world.getName();
        }

        return String.format("%s{X:%f, Y:%f, Z:%f} [Yaw:%f | Pitch:%f]",
                worldName,

                location.getX(),
                location.getY(),
                location.getZ(),

                location.getYaw(),
                location.getPitch());
    }
}
