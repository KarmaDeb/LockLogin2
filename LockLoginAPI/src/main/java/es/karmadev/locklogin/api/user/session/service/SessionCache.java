package es.karmadev.locklogin.api.user.session.service;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

import java.net.InetSocketAddress;

/**
 * Represents the cached data about
 * a session
 */
public interface SessionCache {

    /**
     * Get the client attached to
     * this cache
     *
     * @return the client
     */
    LocalNetworkClient getClient();

    /**
     * Get the address attached to
     * this cache
     *
     * @return the address
     */
    InetSocketAddress getAddress();

    /**
     * Get if the session was logged
     *
     * @return if the session was logged
     */
    boolean isLogged();

    /**
     * Get if the session was totp
     * logged
     *
     * @return if the session was totp
     * logged
     */
    boolean isTotpLogged();

    /**
     * Get if the session was pin
     * logged
     *
     * @return if the session was pin
     * logged
     */
    boolean isPinLogged();
}
