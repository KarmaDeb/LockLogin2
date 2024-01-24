package es.karmadev.locklogin.api.plugin.service.floodgate;

import es.karmadev.locklogin.api.plugin.service.PluginService;

import java.util.UUID;

/**
 * LockLogin floodgate service
 */
public interface FloodGateService extends PluginService {

    /**
     * Get if the client is a bedrock client
     *
     * @param uniqueId the client id
     * @return if the client is bedrock
     */
    boolean isBedrock(final UUID uniqueId);

    /**
     * Remove the prefix and suffix from the
     * client name
     *
     * @param name the client name
     * @return the clean client name
     */
    String clearName(final String name);

    /**
     * Get if the bedrock client is linked
     * with a java client
     *
     * @param uniqueId the client unique id
     * @return if the client is linked with
     * a java client
     */
    boolean isLinked(final UUID uniqueId);
}
