package es.karmadev.locklogin.common.plugin.web.communication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.communication.CommunicationService;
import es.karmadev.locklogin.api.network.communication.data.Channel;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.common.plugin.web.SocketService;
import es.karmadev.locklogin.common.api.packet.CInPacket;
import io.socket.client.Ack;
import io.socket.client.Socket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class WebSocketPacketHandler implements CommunicationService {

    private final Lock lock = new ReentrantLock();
    private final Map<Integer, Integer> packetTries = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> packetConfirmations = new ConcurrentHashMap<>();
    private final Set<Channel> subscribed = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ScheduledExecutorService resendScheduler = new ScheduledThreadPoolExecutor(1);
    private final SocketService service;

    private boolean installed = false;
    private boolean paused = false;

    public BiFunction<Channel, IncomingPacket, Boolean> onPacketSending = null;
    public BiConsumer<Channel, IncomingPacket> onPacketReceive = null;

    /**
     * Initialize the web socket packet handler
     *
     * @param service the service handler
     */
    public WebSocketPacketHandler(final SocketService service) {
        this.service = service;
    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "WebSocket service";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return false;
    }

    /**
     * Install this service
     */
    @Override
    public void install() {
        Socket socket = service.getInstance();

        if (!paused && !installed) {
            installed = true;
            socket.connect();
        }

        if (installed && paused) {
            paused = false;
            for (Channel channel : subscribed) {
                appendListener(channel, socket);
            }
        }
    }

    /**
     * Enable this service
     *
     * @return if the service was able to be
     * enabled
     */
    @Override
    public boolean enable() {
        return paused;
    }

    /**
     * Disable this service
     *
     * @return if the service was able to
     * be disabled
     */
    @Override
    public boolean disable() {
        paused = true;
        Socket socket = service.getInstance();
        socket.offAnyIncoming();

        synchronized (Thread.currentThread()) {
            long start = System.currentTimeMillis();
            while (!socket.connected()) {
                long now = System.currentTimeMillis();
                long diff = now - start;
                if (diff >= 20000) {
                    break;
                }
                try {
                    Thread.currentThread().wait(1000); //Try to wait before the next check, so we avoid overloading the system
                } catch (InterruptedException ignored) {}
            }
        }

        return socket.connected();
    }

    /**
     * Subscribe to a channel
     *
     * @param channels the channels to subscribe
     */
    @Override
    public void subscribe(final Channel... channels) {
        if (!paused && installed) {
            Socket socket = service.getInstance();

            for (Channel channel : channels) {
                if (!socket.hasListeners(channel.name)) {
                    appendListener(channel, socket);
                }
            }
        }
    }

    /**
     * Unsubscribe a channel
     *
     * @param channels the channels to unsubscribe
     */
    @Override
    public void unsubscribe(final Channel... channels) {
        if (!paused && installed) {
            Socket socket = service.getInstance();

            for (Channel channel : channels) {
                socket.off(channel.name);
            }
        }
    }

    /**
     * When a packet has been sent
     *
     * @param channel the receiving channel
     * @param packet  the packet
     */
    @Override
    public void onPacketReceive(final Channel channel, final IncomingPacket packet) {
        if (!paused && installed && onPacketReceive != null) {
            onPacketReceive.accept(channel, packet);
        }
    }

    /**
     * When a packet has been sent
     *
     * @param channel the packet channel
     * @param packet  the packet
     * @return if the packet will be sent
     */
    @Override
    public boolean onPacketSend(final Channel channel, final IncomingPacket packet) {
        if (!paused && installed && onPacketSending != null) {
            return onPacketSending.apply(channel, packet);
        }

        return false;
    }

    /**
     * Send a packet
     *
     * @param channel the channel to send the packet
     *                on
     * @param packet  the packet to send
     * @param priority the packet priority
     */
    @Override
    public void sendPacket(final Channel channel, final OutgoingPacket packet, final long priority) {
        if (!paused && installed) {
            LockLogin plugin = CurrentPlugin.getPlugin();

            int packetId = packet.id();
            int maxTries = (priority <= 25 ? 10 : (priority <= 50 ? 7 : (priority <= 75 ? 5 : 3)));
            int maxTimeout = (priority <= 25 ? 120 : (priority <= 50 ? 60 : (priority <= 75 ? 45 : 30)));

            resendScheduler.schedule(() -> {
                synchronized (lock) {
                    if (packetConfirmations.containsKey(packetId)) {
                        if (!packetConfirmations.get(packetId)) {
                            int tries = packetTries.computeIfAbsent(packetId, (t) -> 0);
                            if (tries++ < maxTries) {
                                sendPacket(channel, packet, priority);
                                packetTries.put(packetId, tries);
                            } else {
                                packetTries.remove(packetId);
                                packetConfirmations.remove(packetId);

                                Path packetStore = plugin.workingDirectory().resolve("cache").resolve("packet").resolve(packetId + ".json");
                                boolean write = false;
                                try {
                                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                    byte[] bytes = gson.toJson(packet.build()).getBytes(StandardCharsets.UTF_8);
                                    PathUtilities.createDirectory(packetStore);

                                    Files.write(packetStore, bytes);
                                    write = true;
                                } catch (IOException ignored) {}

                                if (write) {
                                    plugin.logErr("Failed to send packet to websocket server after 3 tries (30 seconds). The packet will be stored at: {0}", packetId, PathUtilities.pathString(packetStore));
                                } else {
                                    plugin.logErr("Failed to send packet with id {0} to server", packetId);
                                }

                                plugin.err("Failed to send packet {0} after 3 tries. More information at plugin logs", packetId);
                            }
                        }
                    }
                }
            }, maxTimeout, TimeUnit.SECONDS);

            Socket socket = service.getInstance();
            packetConfirmations.put(packetId, false);
            socket.emit(channel.name, packet.build(), (Ack) (data) -> {
                synchronized (lock) {
                    try {
                        int received = Integer.parseInt(String.valueOf(data[0]));
                        packetConfirmations.put(received, true);
                    } catch (IndexOutOfBoundsException | NumberFormatException ex) {
                        Path packetStore = plugin.workingDirectory().resolve("cache").resolve("packet").resolve(packetId + ".json");
                        boolean write = false;
                        try {
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            byte[] bytes = gson.toJson(packet.build()).getBytes(StandardCharsets.UTF_8);
                            PathUtilities.createDirectory(packetStore);

                            Files.write(packetStore, bytes);
                            write = true;
                        } catch (IOException ignored) {}

                        if (write) {
                            plugin.log(ex, "Failed to send packet to websocket server. The packet will be stored at: {0}", PathUtilities.pathString(packetStore));
                        } else {
                            plugin.log(ex, "Failed to send packet to websocket server. The packet had an ID of {0}, and was unable to be stored", packetId);
                        }

                        plugin.err("An error occurred while sending packet with {0}. More information at plugin logs", packetId);
                    }
                }
            });
        }
    }

    /**
     * Append a listener
     *
     * @param channel the channel to listen at
     * @param socket the socket
     */
    private void appendListener(final Channel channel, final Socket socket) {
        if (!paused && installed) {
            LockLogin plugin = CurrentPlugin.getPlugin();

            socket.on(channel.name, (data) -> {
                if (data.length == 1) {
                    Object raw = data[0];
                    if (raw instanceof String) {
                        String rawPacketData = (String) raw;
                        try {
                            CInPacket packet = new CInPacket(rawPacketData);
                            onPacketReceive(channel, packet);
                        } catch (InvalidPacketDataException invalid) {
                            plugin.log(invalid, "Failed to parse websocket packet");
                            plugin.err("An error occurred while processing packet data. Check plugin logs for more information");
                        }
                    }
                }
            });
        }
    }
}
