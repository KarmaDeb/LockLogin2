package es.karmadev.locklogin.spigot.util.storage;

import es.karmadev.api.database.DatabaseManager;
import es.karmadev.api.database.model.JsonDatabase;
import es.karmadev.api.database.model.json.JsonConnection;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Spawn location storage
 */
public final class SpawnLocationStorage {

    private final static LockLogin plugin = CurrentPlugin.getPlugin();
    private final static JsonConnection connection = ((JsonDatabase) DatabaseManager.getEngine("json").orElse(new JsonDatabase())).grabConnection("data/locations/spawn.json");
    private static Location cachedSpawn;

    public static void assign(final @Nullable Location location) {
        if (location == null) {
            cachedSpawn = null;
            connection.set("world", (String) null);
            connection.set("x", (Number) null);
            connection.set("y", (Number) null);
            connection.set("z", (Number) null);
            connection.set("yaw", (Number) null);
            connection.set("pitch", (Number) null);

            connection.save();
            return;
        }

        cachedSpawn = location;
        World world = location.getWorld();
        if (world == null) return;

        connection.set("world", world.getUID().toString());
        connection.set("x", location.getX());
        connection.set("y", location.getY());
        connection.set("z", location.getZ());
        connection.set("yaw", location.getYaw());
        connection.set("pitch", location.getPitch());

        if (connection.save()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                int networkId = UserDataHandler.getNetworkId(player);
                if (networkId > 0) {
                    NetworkClient client = plugin.network().getPlayer(networkId);
                    UserSession session = client.session();
                    if (session.id() > 0 && !session.isLogged()) {
                        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN); //Updated spawn location
                    }
                }
            }
        }
    }

    public static Location load() {
        if (!connection.isSet("world") || !connection.isSet("x") || !connection.isSet("y")
                || !connection.isSet("z") || !connection.isSet("yaw") || !connection.isSet("pitch")) return null;

        if (cachedSpawn != null) return cachedSpawn;

        UUID worldId = UUID.fromString(connection.getString("world"));
        World world = Bukkit.getServer().getWorld(worldId);
        if (world == null) return null;

        double x = connection.getNumber("x").doubleValue();
        double y = connection.getNumber("y").doubleValue();
        double z = connection.getNumber("z").doubleValue();

        Location location = new Location(world, x, y, z);

        float yaw = connection.getNumber("yaw").floatValue();
        float pitch = connection.getNumber("pitch").floatValue();

        location.setYaw(yaw);
        location.setPitch(pitch);

        location.getChunk().load(true); //Preload the chunk
        cachedSpawn = location;

        return location;
    }
}
