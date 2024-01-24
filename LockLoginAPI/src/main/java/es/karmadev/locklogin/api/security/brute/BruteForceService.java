package es.karmadev.locklogin.api.security.brute;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.service.PluginService;

import java.net.InetAddress;

/**
 * LockLogin brute force service
 */
@SuppressWarnings("unused")
public interface BruteForceService extends PluginService {

    /**
     * Success authentication request
     *
     * @param address the address that performed
     *                the success request
     */
    void success(final InetAddress address);

    /**
     * Fail authentication request
     *
     * @param address the address that performed
     *                the failed request
     */
    void fail(final InetAddress address);

    /**
     * Block temporally the address
     *
     * @param address the addres to block
     * @param time    the block time
     */
    void block(final InetAddress address, final long time);

    /**
     * Unblock the address
     *
     * @param address the address to unblock
     */
    void unblock(final InetAddress address);

    /**
     * Get the blocked addresses
     *
     * @return the blocked addresses
     */
    InetAddress[] blocked();

    /**
     * Toggle the panic mode on the client
     *
     * @param client the client
     * @param status the panic mode status
     */
    void togglePanic(final LocalNetworkClient client, boolean status);

    /**
     * Get the authentication tries of
     * an address
     *
     * @param address the address
     * @return the address authentication requests
     */
    int tries(final InetAddress address);

    /**
     * Get the address temporal ban time left
     *
     * @param address the address
     * @return the address ban time left
     */
    long banTimeLeft(final InetAddress address);

    /**
     * Get if the client is panicking
     *
     * @param client the client
     * @return if panicking
     */
    boolean isPanicking(final LocalNetworkClient client);

    /**
     * Get if the address is blocked
     *
     * @param address the address
     * @return if the address is blocked
     */
    boolean isBlocked(final InetAddress address);

    /**
     * Save the current brute force
     * status, to be loaded in the next
     * server start
     */
    void saveStatus();

    /**
     * Load the brute force status
     */
    void loadStatus();
}
