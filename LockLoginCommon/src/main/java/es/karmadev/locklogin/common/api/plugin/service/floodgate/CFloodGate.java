package es.karmadev.locklogin.common.api.plugin.service.floodgate;

import es.karmadev.locklogin.api.plugin.service.floodgate.FloodGateService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * LockLogin floodgate service
 */
public class CFloodGate implements FloodGateService {

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "FloodGate";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return false;
    }

    /**
     * Get if the client is a bedrock client
     *
     * @param uniqueId the client id
     * @return if the client is bedrock
     */
    @Override
    public boolean isBedrock(final UUID uniqueId) {
        try {
            Class<?> floodGateAPI = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = floodGateAPI.getDeclaredMethod("getInstance");

            Object instance = getInstance.invoke(floodGateAPI);
            Method isFloodGatePlayer = instance.getClass().getMethod("isFloodgatePlayer", UUID.class);

            return (boolean) isFloodGatePlayer.invoke(instance, uniqueId);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException ignored) {}

        return false;
    }

    /**
     * Remove the prefix and suffix from the
     * client name
     *
     * @param name the client name
     * @return the clean client name
     */
    @Override
    public String clearName(final String name) {
        try {
            Class<?> floodGateAPI = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = floodGateAPI.getDeclaredMethod("getInstance");

            Object instance = getInstance.invoke(floodGateAPI);
            Method getPlayerPrefix = instance.getClass().getMethod("getPlayerPrefix");

            String prefix = (String) getPlayerPrefix.invoke(instance);
            if (name.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException ignored) {}

        return name;
    }

    /**
     * Get if the bedrock client is linked
     * with a java client
     *
     * @param uniqueId the client unique id
     * @return if the client is linked with
     * a java client
     */
    @Override
    public boolean isLinked(final UUID uniqueId) {
        try {
            Class<?> floodGateAPI = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = floodGateAPI.getDeclaredMethod("getInstance");

            Object instance = getInstance.invoke(floodGateAPI);
            Method getPlayer = instance.getClass().getMethod("getPlayer", UUID.class);

            Object player = getPlayer.invoke(instance, uniqueId);
            if (player != null) {
                Method getLinkedPlayer = player.getClass().getMethod("getLinkedPlayer");
                Object linkedPlayer = getLinkedPlayer.invoke(player);

                return linkedPlayer != null;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException ignored) {}

        return false;
    }
}
