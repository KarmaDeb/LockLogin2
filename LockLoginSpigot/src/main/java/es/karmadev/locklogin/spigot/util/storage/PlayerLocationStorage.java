package es.karmadev.locklogin.spigot.util.storage;

import es.karmadev.api.database.DatabaseManager;
import es.karmadev.api.database.model.JsonDatabase;
import es.karmadev.api.database.model.json.JsonConnection;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Player location storage
 */
public class PlayerLocationStorage {

    private final static JsonDatabase database = (JsonDatabase) DatabaseManager.getEngine("json").orElse(new JsonDatabase());
    private final JsonConnection connection;

    public PlayerLocationStorage(final NetworkClient client) {
        connection = database.grabConnection("data/locations/" + client.id() + ".json");
    }

    public void assign(final Location location) {
        World world = location.getWorld();
        if (world == null) return;

        connection.set("world", world.getUID().toString());
        connection.set("x", location.getX());
        connection.set("y", location.getY());
        connection.set("z", location.getZ());
        connection.set("yaw", location.getYaw());
        connection.set("pitch", location.getPitch());
        connection.save();
    }

    public Location load() {
        if (!connection.isSet("world") || !connection.isSet("x") || !connection.isSet("y")
                || !connection.isSet("z") || !connection.isSet("yaw") || !connection.isSet("pitch")) return null;

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

        return location;
    }
}
