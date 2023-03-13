package es.karmadev.locklogin.common.api.client;

import com.google.gson.JsonElement;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.common.api.SQLiteDriver;
import es.karmadev.locklogin.common.api.server.channel.SPacket;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

public final class COnlineClient extends CLocalClient implements NetworkClient {

    public Function<String, Void> sendMessage;
    public Function<String, Void> sendActionbar;
    public Function<ClientTitle, Void> sendTitle;
    public Function<String[], Void> kick;
    public Function<String, Void> performCommand;

    private NetworkServer server;
    private NetworkServer previous = null;

    public COnlineClient(final int id, final SQLiteDriver pool, final NetworkServer current) {
        super(id, pool);
        server = current;
    }

    /**
     * Get the client previous server
     *
     * @return the client previous server
     */
    @Override
    public NetworkServer previousServer() {
        return previous;
    }

    /**
     * Get the client current server
     *
     * @return the client server
     */
    @Override
    public NetworkServer server() {
        return server;
    }

    /**
     * Send a message to the client
     *
     * @param message the message to send
     */
    @Override
    public void sendMessage(final String message) {
        if (sendMessage != null) sendMessage.apply(message);
    }

    /**
     * Send an actionbar to the client
     *
     * @param actionbar the actionbar to send
     */
    @Override
    public void sendActionBar(final String actionbar) {
        if (sendActionbar != null) sendActionbar.apply(actionbar);
    }

    /**
     * Send a title to the client
     *
     * @param title    the title
     * @param subtitle the subtitle
     * @param fadeIn   the title fade in time
     * @param showTime the title show time
     * @param fadeOut  the title fade out time
     */
    @Override
    public void sendTitle(final String title, final String subtitle, final int fadeIn, final int showTime, final int fadeOut) {
        if (sendTitle != null) sendTitle.apply(ClientTitle.on(title, subtitle, fadeIn, showTime, fadeOut));
    }

    /**
     * Kick a client
     *
     * @param reason the kick reason
     */
    @Override
    public void kick(final String... reason) {
        if (kick != null) kick.apply(reason);
    }

    /**
     * Send a packet to the current client server
     *
     * @param priority the packet priority
     * @param data     the packet data
     * @throws SecurityException if there's no module trying to send the packet
     */
    @Override
    public void appendPacket(final int priority, final byte... data) throws SecurityException {
        if (server != null) server.channel().appendPacket(new SPacket(data).priority(priority));
    }

    /**
     * Send a packet to the current client server
     *
     * @param priority the packet priority
     * @param data     the packet data
     * @throws SecurityException if there's no module or plugin trying to send the packet
     */
    @Override
    public void appendPacket(final int priority, final String data) throws SecurityException {
        if (server != null) server.channel().appendPacket(new SPacket(data).priority(priority));
    }

    /**
     * Send a packet to the current client server
     *
     * @param priority the packet priority
     * @param data     the packet data
     * @throws SecurityException if there's no module or plugin trying to send the packet
     */
    @Override
    public void appendPacket(final int priority, final JsonElement data) throws SecurityException {
        if (server != null) server.channel().appendPacket(new SPacket(data).priority(priority));
    }

    /**
     * Perform a command
     *
     * @param cmd the command to perform
     */
    @Override
    public void performCommand(final String cmd) {
        CurrentPlugin.getPlugin().runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        if (performCommand != null) performCommand.apply(cmd);
    }

    /**
     * Get if the client is online
     *
     * @return if the client is online
     */
    @Override
    public boolean online() {
        return true;
    }

    /**
     * Get the network client
     *
     * @return the network client
     */
    @Override
    public NetworkClient client() {
        return this;
    }

    /**
     * Set the client server
     *
     * @param server the server to set on
     *               If the client is online, we will move him
     *               to this server, otherwise he will join it
     *               when he joins the server
     */
    @Override
    public void setServer(final NetworkServer server) {
        previous = this.server;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `user` SET `previous_server` = " + previous.id() + " WHERE `id` = " + id);
            statement.executeUpdate("UPDATE `user` SET `last_server` = " + server.id() + " WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection ,statement);
        }

        this.server = server;
    }

    /**
     * Force the client previous server
     *
     * @param server the new previous server
     */
    @Override
    public void forcePreviousServer(final NetworkServer server) {
        previous = server;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate("UPDATE `user` SET `previous_server` = " + server.id() + " WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection ,statement);
        }
    }
}
