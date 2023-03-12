package es.karmadev.locklogin.common.plugin.web;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.CommunicationSection;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import ml.karmaconfigs.api.common.string.StringUtils;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.socket.client.IO.Options;

/**
 * LockLogin websocket service
 */
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
        if (StringUtils.isNullOrEmpty(host) || port <= 0 || port > 65_535) {
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
