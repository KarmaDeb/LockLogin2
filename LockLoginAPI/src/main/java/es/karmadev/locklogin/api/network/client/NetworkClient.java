package es.karmadev.locklogin.api.network.client;

import com.google.gson.JsonElement;
import es.karmadev.locklogin.api.network.TextContainer;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.session.check.SessionChecker;

/**
 * Network client
 */
@SuppressWarnings("unused")
public interface NetworkClient extends LocalNetworkClient, TextContainer {

    /**
     * Kick a client
     *
     * @param reason the kick reason
     */
    void kick(final String... reason);

    /**
     * Send a packet to the current client server
     *
     * @param priority the packet priority
     * @param data the packet data
     * @throws SecurityException if there's no module or plugin trying to send the packet
     */
    void appendPacket(final int priority, final byte... data) throws SecurityException;

    /**
     * Send a packet to the current client server
     *
     * @param priority the packet priority
     * @param data the packet data
     * @throws SecurityException if there's no module or plugin trying to send the packet
     */
    void appendPacket(final int priority, final String data) throws SecurityException;

    /**
     * Send a packet to the current client server
     *
     * @param priority the packet priority
     * @param data the packet data
     * @throws SecurityException if there's no module or plugin trying to send the packet
     */
    void appendPacket(final int priority, final JsonElement data) throws SecurityException;

    /**
     * Send a packet to the current client server
     *
     * @param data the packet data
     * @throws SecurityException if there's no module or plugin trying to send the packet
     */
    default void appendPacket(final byte... data) throws SecurityException {
        appendPacket(Integer.MAX_VALUE, data);
    }

    /**
     * Send a packet to the current client server
     *
     * @param data the packet data
     * @throws SecurityException if there's no module or plugin trying to send the packet
     */
    default void appendPacket(final String data) throws SecurityException {
        appendPacket(Integer.MAX_VALUE, data);
    }

    /**
     * Send a packet to the current client server
     *
     * @param data the packet data
     * @throws SecurityException if there's no module or plugin trying to send the packet
     */
    default void appendPacket(final JsonElement data) throws SecurityException {
        appendPacket(Integer.MAX_VALUE, data);
    }

    /**
     * Perform a command
     *
     * @param cmd the command to perform
     */
    void performCommand(final String cmd);

    /**
     * Get the client session checker
     *
     * @return the client session checker
     */
    SessionChecker getSessionChecker();
}
