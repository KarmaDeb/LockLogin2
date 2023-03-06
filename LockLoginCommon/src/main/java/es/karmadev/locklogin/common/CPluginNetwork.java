package es.karmadev.locklogin.common;

import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CPluginNetwork implements PluginNetwork {

    private final Set<LocalNetworkClient> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<NetworkServer> servers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Append a client
     *
     * @param client the client to append
     */
    public void appendClient(final LocalNetworkClient client) {
        clients.add(client);
    }

    /**
     * Append a server
     *
     * @param server the server to append
     */
    public void appendServer(final NetworkServer server) {
        servers.add(server);
    }

    /**
     * Get a client
     *
     * @param id the client id
     * @return the client
     */
    @Override
    public NetworkClient getPlayer(final int id) {
        Optional<LocalNetworkClient> offline = clients.stream().filter((client) -> client.id() == id).findFirst();
        if (offline.isPresent()) {
            LocalNetworkClient local = offline.get();
            if (local.online()) return local.client();
        }

        return null;
    }

    /**
     * Get a client
     *
     * @param name the client name
     * @return the client
     */
    @Override
    public NetworkClient getPlayer(final String name) {
        Optional<LocalNetworkClient> offline = clients.stream().filter((cl) -> cl.name().equals(name)).findFirst();
        if (offline.isPresent()) {
            LocalNetworkClient local = offline.get();
            if (local.online()) return local.client();
        }

        return null;
    }

    /**
     * Get a client
     *
     * @param uniqueId the client unique id
     * @return the client
     */
    @Override
    public NetworkClient getPlayer(final UUID uniqueId) {
        Optional<LocalNetworkClient> offline = clients.stream().filter((cl) -> cl.uniqueId().equals(uniqueId)).findFirst();
        if (offline.isPresent()) {
            LocalNetworkClient local = offline.get();
            if (local.online()) return local.client();
        }

        return null;
    }

    /**
     * Get a client
     *
     * @param id the client id
     * @return the client
     */
    @Override
    public LocalNetworkClient getEntity(final int id) {
        return clients.stream().filter((client) -> client.id() == id).findFirst().orElse(null);
    }

    /**
     * Get an offline client
     *
     * @param uniqueId the client unqiue id
     * @return the client
     */
    @Override
    public LocalNetworkClient getOfflinePlayer(final UUID uniqueId) {
        return clients.stream().filter((client) -> client.uniqueId() == uniqueId).findFirst().orElse(null);
    }

    /**
     * Get a server
     *
     * @param id the server id
     * @return the server
     */
    @Override
    public NetworkServer getServer(final int id) {
        return servers.stream().filter((server) -> server.id() == id).findFirst().orElse(null);
    }

    /**
     * Get a server
     *
     * @param name the server name
     * @return the server
     */
    @Override
    public NetworkServer getServer(final String name) {
        return servers.stream().filter((server) -> server.name().equals(name)).findFirst().orElse(null);
    }

    /**
     * Get all the online players
     *
     * @return the online players
     */
    @Override
    public Collection<NetworkEntity> getOnlinePlayers() {
        List<NetworkEntity> entities = new ArrayList<>();
        clients.forEach((cl) -> {
            if (cl.online()) entities.add(cl.client());
        });

        return entities;
    }

    /**
     * Get all the players
     *
     * @return all the players
     */
    @Override
    public Collection<LocalNetworkClient> getPlayers() {
        return new ArrayList<>(clients);
    }

    /**
     * Get all the servers
     *
     * @return the servers
     */
    @Override
    public Collection<NetworkServer> getServers() {
        return new ArrayList<>(servers);
    }
}
