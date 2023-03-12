package es.karmadev.locklogin.api.network.server;

import java.net.InetSocketAddress;

/**
 * LockLogin server factory
 *
 * @param <T> the server
 */
public interface ServerFactory<T extends NetworkServer> {

    /**
     * Create a new server
     *
     * @param name the server name
     * @param address the server address
     * @return the server
     */
    T create(final String name, final InetSocketAddress address);
}
