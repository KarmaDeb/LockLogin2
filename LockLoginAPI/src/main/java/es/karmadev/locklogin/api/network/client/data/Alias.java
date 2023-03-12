package es.karmadev.locklogin.api.network.client.data;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

/**
 * Alias
 */
public interface Alias {

    /**
     * Get the alias name
     *
     * @return the alias name
     */
    String name();

    /**
     * Get the clients in the alias
     *
     * @return the clients in the alias
     */
    LocalNetworkClient[] clients();

    /**
     * Add the clients to the alias
     *
     * @param clients the clients to add
     */
    void add(final LocalNetworkClient... clients);

    /**
     * Remove the clients from the alias
     *
     * @param clients the clients to remove
     */
    void remove(final LocalNetworkClient... clients);
}
