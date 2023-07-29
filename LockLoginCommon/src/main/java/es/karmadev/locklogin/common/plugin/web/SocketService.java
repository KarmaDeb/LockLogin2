package es.karmadev.locklogin.common.plugin.web;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.CommunicationSection;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import okhttp3.OkHttpClient;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.socket.client.IO.Options;

/**
 * LockLogin websocket service
 */
@SuppressWarnings("unused")
public class SocketService {

    private static SocketService service;

    private final URI socket_address;

    private Socket instance;

    public static SocketService getService() {
        return service;
    }

    public SocketService() {
        if (service == null) service = this;
        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();
        CommunicationSection section = configuration.communications();

        String protocol = "wss"; //Default protocol
        String host = section.host();
        int port = section.port();

        if (!section.useSSL()) {
            protocol = "ws";
        }
        if (ObjectUtils.isNullOrEmpty(host) || port <= 0 || port > 65_535) {
            protocol = "wss"; //Default protocol
            host = "karmadev.es"; //Default host
            port = 2053; //Default port

            plugin.warn("Invalid communication server configuration detected, using defaults");
        }

        socket_address = URI.create(protocol + "://" + host + ":" + port);
    }

    /**
     * Get the socket address
     *
     * @return the socket address
     */
    public URI getAddress() {
        return socket_address;
    }

    public Socket getInstance() {
        if (instance != null) return instance;

        Options options = new Options();
        options.secure = false;
        options.multiplex = false;
        options.forceNew = false;
        options.transports = new String[]{Polling.NAME, WebSocket.NAME};
        options.upgrade = true;
        options.rememberUpgrade = true;
        options.reconnection = true;
        options.reconnectionAttempts = 5;
        options.reconnectionDelay = 1;
        options.reconnectionDelayMax = 5;
        options.randomizationFactor = 0.5;
        options.auth = new ConcurrentHashMap<>();

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.MINUTES)
                .build();

        options.callFactory = client;
        options.webSocketFactory = client;

        instance = IO.socket(socket_address, options);
        return instance;
    }
}
