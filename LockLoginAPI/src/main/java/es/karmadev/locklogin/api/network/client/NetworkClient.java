package es.karmadev.locklogin.api.network.client;

import com.google.gson.JsonElement;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

/**
 * Network client
 */
@SuppressWarnings("unused")
public interface NetworkClient extends LocalNetworkClient {

    /**
     * Send a message to the client
     *
     * @param message the message to send
     */
    void sendMessage(final String message);

    /**
     * Send an actionbar to the client
     *
     * @param actionbar the actionbar to send
     */
    void sendActionBar(final String actionbar);

    /**
     * Send a title to the client
     *
     * @param title the title
     * @param subtitle the subtitle
     * @param fadeIn the title fade in time
     * @param showTime the title show time
     * @param fadeOut the title fade out time
     */
    void sendTitle(final String title, final String subtitle, final int fadeIn, final int showTime, final int fadeOut);

    /**
     * Send a title to the client
     *
     * @param title the title to send
     * @param subtitle the subtitle
     */
    default void sendTitle(final String title, final String subtitle) {
        sendTitle(title, subtitle, 2, 5, 2);
    }

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
}
