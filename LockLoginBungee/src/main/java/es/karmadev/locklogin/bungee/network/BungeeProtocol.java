package es.karmadev.locklogin.bungee.network;

import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.NetworkEvent;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.PacketReceiveEvent;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.network.ChannelProviderService;
import es.karmadev.locklogin.common.api.packet.COutPacket;
import es.karmadev.locklogin.common.api.packet.frame.CFramePacket;
import es.karmadev.locklogin.protocol.ProtocolHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.PluginMessage;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class BungeeProtocol extends ProtocolHandler {

    private final static LockLogin plugin = CurrentPlugin.getPlugin();

    /**
     * Initialize a new protocol handler
     *
     * @param pair the protocol handler
     *             key pair. The key pair is used
     *             when communicating, to encrypt
     *             and decrypt the data
     * @param pairAlgorithm the key pair algorithm
     * @param secret the protocol handler secret key
     * @param secretAlgorithm the secret key algorithm
     * @throws NoSuchAlgorithmException if the secret key generation fails. The
     * secret key is strictly required to encrypt data sent by
     * the protocol
     */
    public BungeeProtocol(final KeyPair pair, final String pairAlgorithm, final SecretKey secret, final String secretAlgorithm) throws NoSuchAlgorithmException {
        super(pair, pairAlgorithm, secret, secretAlgorithm);
    }

    /**
     * Handle a packet
     *
     * @param channel the channel
     * @param packet the packet
     */
    @Override
    protected void handle(final String channel, final IncomingPacket packet) {
        DataType type = packet.getType();
        if (type.equals(DataType.CONNECTION_INIT)) {
            plugin.logInfo("Successfully established a secure connection with ", channel);

            OutgoingPacket config = new COutPacket(DataType.SHARED_MESSAGES);
            config.addProperty("messages", Base64.getEncoder().encodeToString(plugin.configuration().serialize().getBytes()));

            String tag = String.format("%s_%s",
                    StringUtils.generateString(4), StringUtils.generateString(6));
            write(channel, tag, config);
            return;
        }

        String virtualTag = packet.getSequence("v_tag");
        if (virtualTag == null) return;

        PluginService service = plugin.getService("plugin-messaging");
        if (!(service instanceof ChannelProviderService)) return;

        ChannelProviderService provider = (ChannelProviderService) service;
        if (provider.getChannels().stream().noneMatch((ch) -> ch.getChannel().equals(virtualTag))) return;

        provider.getChannel(virtualTag).whenComplete(((networkChannel, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }

            if (networkChannel == null) return;

            NetworkEvent event = new PacketReceiveEvent(networkChannel, packet);
            networkChannel.handle(event);
        }));
    }

    /**
     * Write a packet. The packet parsed in this method should be
     * already encoded.
     * Implementation must split the packet into frames, otherwise
     * the other part won't take the packet. The reason for why we
     * don't parse an already built in packet frame array is because
     * we want to let implementations choose how and how big the frames
     * are.
     *
     * @param channel the channel
     * @param packetId the packet id being written
     * @param tag the tag
     * @param data the packet to write
     */
    @Override
    protected void emit(final String channel, final long packetId, final String tag, final byte[] data) {
        Server server = locateServer(channel);
        if (server == null) return;

        if (data.length <= ProtocolHandler.BLOCK_SIZE) {
            PacketFrame frame = new CFramePacket(packetId, 1, 1, data, Instant.now());
            byte[] rawFrame = StringUtils.serialize(frame).getBytes();

            DefinedPacket defined = new PluginMessage(tag, rawFrame, true);
            server.unsafe().sendPacket(defined);
            return;
        }

        List<byte[]> splitData = new ArrayList<>();
        int blocks = data.length / ProtocolHandler.BLOCK_SIZE;
        int remainingBytes = data.length % ProtocolHandler.BLOCK_SIZE;

        for (int i = 0; i < blocks; i++) {
            byte[] block = new byte[ProtocolHandler.BLOCK_SIZE];
            System.arraycopy(data, i * ProtocolHandler.BLOCK_SIZE, block, 0, ProtocolHandler.BLOCK_SIZE);

            splitData.add(block);
        }

        if (remainingBytes > 0) {
            byte[] finalBlock = new byte[remainingBytes];
            System.arraycopy(data, blocks * ProtocolHandler.BLOCK_SIZE, finalBlock, 0, remainingBytes);

            splitData.add(finalBlock);
        }

        int position = 1;
        for (byte[] target : splitData) {
            CFramePacket frame = new CFramePacket(packetId, position++, data.length, target, Instant.now());
            String serial = StringUtils.serialize(frame);
            byte[] serialData = serial.getBytes();

            DefinedPacket message = new PluginMessage(tag, serialData, false);
            server.unsafe().sendPacket(message);
        }
    }

    private Server locateServer(final String channel) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player == null || !player.isConnected()) continue;

            Server server = player.getServer();
            if (server == null) continue;

            ServerInfo info = server.getInfo();
            if (info.getName().equals(channel)) return server;
        }

        return null;
    }
}
