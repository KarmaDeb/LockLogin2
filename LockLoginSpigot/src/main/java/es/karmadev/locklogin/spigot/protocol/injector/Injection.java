package es.karmadev.locklogin.spigot.protocol.injector;

import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.kson.io.JsonReader;
import es.karmadev.api.spigot.server.SpigotServer;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.FrameBuilder;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.PacketReceiveEvent;
import es.karmadev.locklogin.common.api.packet.CInPacket;
import es.karmadev.locklogin.common.api.packet.COutPacket;
import es.karmadev.locklogin.common.api.packet.frame.CFrameBuilder;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.Getter;
import org.bukkit.entity.Player;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin client injection
 */
public class Injection extends ChannelDuplexHandler {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private static Field playerConnectionField;
    private static Field networkManagerField;
    private static Field channelField;

    static PublicKey sharedPublic;

    @Getter
    private boolean injected = false;
    private Object playerConnection;
    private Channel channel = null;

    private final Map<String, FrameBuilder> frames = new ConcurrentHashMap<>();

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     * <p>
     * Sub-classes may override this method to change behavior.
     *
     * @param context the context
     * @param packet the packet
     */
    @Override
    public void channelRead(final ChannelHandlerContext context, Object packet) throws Exception {
        String name = packet.getClass().getSimpleName();
        if (name.contains("CustomPayload")) {
            Field[] fields = packet.getClass().getDeclaredFields();
            String identifierField = null;
            String dataField = null;

            Object readFrom = packet;
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                Class<?> fieldType = field.getType();
                String ftName = fieldType.getSimpleName();

                if (ftName.equalsIgnoreCase("String") || ftName.toLowerCase().contains("key")) {
                    identifierField = field.getName();
                }
                if (ftName.equalsIgnoreCase("PacketDataSerializer")) {
                    dataField = field.getName();
                }

                if (ftName.equalsIgnoreCase("CustomPacketPayload")) {
                    field.setAccessible(true);
                    Object customPacketPayload = field.get(packet);

                    if (customPacketPayload.getClass().getSimpleName().equalsIgnoreCase("UnknownPayload")) {
                        readFrom = customPacketPayload;

                        identifierField = "id";
                        dataField = "data";
                        break;
                    }
                }
            }

            if (identifierField == null || dataField == null) return;
            Object identifierObject = getField(readFrom, identifierField);
            Object dataObject = getField(readFrom, dataField);

            if (identifierObject == null || dataObject == null) return;
            String identifier = identifierObject.toString();

            byte[] data;
            try {
                ByteBuf buf = (ByteBuf) dataObject;
                data = new byte[buf.readableBytes()];
                buf.readBytes(data);

                buf.setIndex(0, 0);
            } catch (ClassCastException ex) {
                return;
            }

            String rawData = new String(data, StandardCharsets.UTF_8);
            Object object = StringUtils.load(rawData).orElse(null);

            if (object instanceof PacketFrame) {
                PacketFrame frame = (PacketFrame) object;
                FrameBuilder builder = frames.computeIfAbsent(identifier, (b) -> new CFrameBuilder((rawPacket) -> {
                    KeyPair pair = plugin.getCommunicationKeys();

                    try {
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
                        return cipher.doFinal(rawPacket);
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                             IllegalBlockSizeException |
                             BadPaddingException ignored) {
                    }

                    return rawPacket;
                }));

                byte[] encrypted = new byte[frame.length()];
                frame.read(encrypted, 0);

                builder.append(frame);

                int position = frame.position();
                int max = frame.frames();

                if (position == max) {
                    try {
                        byte[] rawPacket = builder.build();

                        String raw = new String(rawPacket, StandardCharsets.UTF_8);
                        Object comPacket = StringUtils.load(raw).orElse(null);

                        if (comPacket instanceof OutgoingPacket) {
                            OutgoingPacket out = (OutgoingPacket) comPacket;
                            JsonObject outBuild = out.build();

                            outBuild.put("identifier", identifier);
                            outBuild.put("replying", out.id());

                            raw = outBuild.toString(false);

                            IncomingPacket converted = new CInPacket(raw);
                            NetworkChannel ch = plugin.getChannel(identifier);

                            if (ch == null) {
                                throw new IllegalStateException("Received a packet from unregistered channel: " + identifier);
                            }

                            ch.handle(
                                    new PacketReceiveEvent(ch, converted)
                            );
                        }

                        if (comPacket instanceof IncomingPacket) {
                            IncomingPacket incoming = (IncomingPacket) comPacket;
                            byte[] inData = incoming.getData();

                            JsonObject obj = JsonReader.parse(inData).asObject();
                            obj.put("identifier", identifier);
                            obj.put("replying", incoming.id());

                            String rawJson = obj.toString(false);
                            incoming = new CInPacket(rawJson);

                            plugin.onReceive(incoming);
                        }
                    } catch (InvalidPacketDataException ex) {
                        plugin.log(ex, "Failed to handle packet under tag {0}", identifier);
                    }
                }

                return;
            }

            if (object instanceof OutgoingPacket) {
                OutgoingPacket out = (COutPacket) object;
                JsonObject outBuild = out.build();

                if (out.getType().equals(DataType.HELLO)) {
                    out.addProperty("identifier", identifier);
                    byte[] rawKey = Base64.getDecoder().decode(outBuild.getChild("key").asNative().getString());
                    KeyFactory factory = KeyFactory.getInstance("RSA");
                    EncodedKeySpec keySpec = new X509EncodedKeySpec(rawKey);
                    sharedPublic = factory.generatePublic(keySpec);

                    outBuild.put("identifier", identifier);
                    outBuild.put("replying", out.id());

                    String raw = outBuild.toString(false);

                    IncomingPacket converted = new CInPacket(raw);
                    plugin.onReceive(converted);
                }

                return;
            }
        }

        super.channelRead(context, packet);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    protected void inject(final Player player) {
        if (!injected) {
            if (player == null) return;

            this.channel = getChannel(player);
            if (channel == null) return;

            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addBefore("packet_handler", "LockLogin:" + player.getName(), this);
            injected = true;
        }
    }

    protected void release() {
        if (injected) {
            if (channel != null) {
                EventLoop loop = channel.eventLoop();
                loop.submit(() -> channel.pipeline().remove(this));
            }

            injected = false;
            channel = null;
            playerConnection = null;
        }
    }

    private static Object toCraftPlayer(final Player player) {
        Class<?> craftPlayer = SpigotServer.orgBukkitCraftbukkit("entity.CraftPlayer").orElse(null);
        if (craftPlayer == null) return null;

        try {
            return craftPlayer.cast(player);
        } catch (ClassCastException | SecurityException ex) {
            return null;
        }
    }

    /**
     * Build the player into the player
     * entity handle
     *
     * @param player the player
     * @return the entity handle
     */
    protected static Object toEntityHandle(final Player player) {
        Object craftPlayer = toCraftPlayer(player);
        if (craftPlayer == null) return null;

        try {
            Method getHandle = craftPlayer.getClass().getMethod("getHandle");
            return getHandle.invoke(craftPlayer);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | SecurityException ex) {
            return null;
        }
    }

    protected static Object playerConnection(final Object entityHandle) {
        if (playerConnectionField == null) {
            Class<?> playerConnection = SpigotServer.netMinecraftServer("PlayerConnection").orElseGet(() ->
                    SpigotServer.netMinecraftServer("ServerCommonPacketListenerImpl", "server", "network").orElseGet(() ->
                            SpigotServer.netMinecraftServer("PlayerConnection", "server", "network").orElse(null))
            );
            if (playerConnection == null) return null;

            Class<?> source = entityHandle.getClass();
            while (source != null) {
                Field[] fields = source.getDeclaredFields();
                for (Field field : fields) {
                    Class<?> fieldType = field.getType();
                    if (playerConnection.isAssignableFrom(fieldType)) {
                        playerConnectionField = field;
                        break;
                    }
                }

                source = source.getSuperclass();
            }
        }

        if (playerConnectionField == null) return null;
        try {
            playerConnectionField.setAccessible(true);
            return playerConnectionField.get(entityHandle);
        } catch (IllegalAccessException | SecurityException ex) {
            return null;
        }
    }

    private Object networkManager(final Object playerConnection) {
        if (networkManagerField == null) {
            Class<?> networkManager = SpigotServer.netMinecraftServer("NetworkManager").orElseGet(() ->
                    SpigotServer.netMinecraftServer("NetworkManager", "network").orElse(null)
            );
            if (networkManager == null) return null;

            Class<?> source = playerConnection.getClass();
            while (source != null) {
                Field[] fields = source.getDeclaredFields();
                for (Field field : fields) {
                    Class<?> fieldType = field.getType();
                    if (fieldType.equals(networkManager)) {
                        networkManagerField = field;
                        break;
                    }
                }

                source = source.getSuperclass();
            }
        }

        if (networkManagerField == null) return null;
        try {
            networkManagerField.setAccessible(true);
            return networkManagerField.get(playerConnection);
        } catch (IllegalAccessException | SecurityException ex) {
            return null;
        }
    }

    public Channel getChannel(final Player player) {
        if (playerConnection == null) {
            Object entityHandle = toEntityHandle(player);
            if (entityHandle == null) return null;

            playerConnection = playerConnection(entityHandle);
        }
        if (playerConnection == null) return null;

        Object networkManager = networkManager(playerConnection);
        if (networkManager == null) return null;

        if (channelField == null) {
            Class<Channel> channel = Channel.class;
            Class<?> source = networkManager.getClass();

            while (source != null) {
                Field[] fields = source.getDeclaredFields();
                for (Field field : fields) {
                    Class<?> fieldType = field.getType();
                    if (fieldType.equals(channel)) {
                        channelField = field;
                        break;
                    }
                }

                source = source.getSuperclass();
            }
        }

        if (channelField == null) return null;
        try {
            channelField.setAccessible(true);
            return (Channel) channelField.get(networkManager);
        } catch (IllegalAccessException | SecurityException ex) {
            return null;
        }
    }

    private Object getField(final Object object, final String field) {
        try {
            Field f = object.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(object);
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException ex) {
            return null;
        }
    }
}