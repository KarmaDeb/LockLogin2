package es.karmadev.locklogin.api.user.premium;

import java.util.UUID;

/**
 * LockLogin premium data store
 */
public interface PremiumDataStore {

    /**
     * Get the client id
     *
     * @param name the client name
     * @return the client id
     */
    UUID onlineId(final String name);

    /**
     * Save the client online id
     *
     * @param name the client name
     * @param onlineId the client online id
     */
    void saveId(final String name, final UUID onlineId);
}
