package es.karmadev.locklogin.api.network.client.offline;

import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.event.ClientEvent;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.user.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionField;
import es.karmadev.locklogin.api.user.session.UserSession;

import java.util.UUID;

/**
 * Local network client. This one is local because we
 * expect it to be not online, which basically means its
 * data is being loaded locally
 */
public interface LocalNetworkClient extends NetworkEntity {

    /**
     * Get the entity id
     *
     * @return the entity id
     */
    int id();

    /**
     * Get the connection unique identifier
     *
     * @return the connection unique identifier
     */
    UUID uniqueId();

    /**
     * Update the client unique ID
     *
     * @param id the client unique ID
     */
    void setUniqueId(final UUID id);

    /**
     * Get the client connection
     *
     * @return the client connection type
     */
    ConnectionType connection();

    /**
     * Set the client connection type
     *
     * @param type the connection type
     */
    void setConnection(final ConnectionType type);

    /**
     * Get if the client is online
     *
     * @return if the client is online
     */
    boolean online();

    /**
     * Get the network client
     *
     * @return the network client
     */
    NetworkClient client();

    /**
     * Get the client last server
     *
     * @return the last server
     */
    NetworkServer lastServer();

    /**
     * Get the client account
     *
     * @return the client account
     */
    UserAccount account();

    /**
     * Get the client session
     *
     * @return the client session
     */
    UserSession session();

    /**
     * Set the client server
     *
     * @param server the server to set on
     * If the client is online, we will move him
     * to this server, otherwise he will join it
     * when he joins the server
     */
    void setServer(final NetworkServer server);

    /**
     * Trigger an event
     *
     * @param event the event to fire
     */
    void triggerEvent(final ClientEvent event);
}
