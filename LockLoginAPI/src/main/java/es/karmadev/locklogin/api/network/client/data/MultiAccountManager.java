package es.karmadev.locklogin.api.network.client.data;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;

/**
 * LockLogin mutli account manager
 */
public interface MultiAccountManager {

    /**
     * Assign the address to the
     * specified account
     *
     * @param address the address
     */
    void assign(final LocalNetworkClient client, final InetAddress address);

    /**
     * Get if the connection to this address
     * is allowed
     *
     * @param address the address
     * @param max     the max amount of accounts
     * @return if the address is allowed
     */
    boolean allow(final InetAddress address, final int max);

    /**
     * Get the history of accounts based
     * on the address
     *
     * @param address the address
     * @return the address accounts
     */
    Collection<LocalNetworkClient> getAccounts(final InetAddress address);

    /**
     * Get the history of accounts based on
     * the unique id
     *
     * @param unique_id the unique id
     * @return the unique id account history
     */
    Collection<LocalNetworkClient> getAccounts(final UUID unique_id);
}
