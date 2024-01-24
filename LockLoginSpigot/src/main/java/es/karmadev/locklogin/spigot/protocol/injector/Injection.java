package es.karmadev.locklogin.spigot.protocol.injector;

import es.karmadev.api.spigot.server.SpigotServer;
import es.karmadev.api.strings.StringOptions;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;
import es.karmadev.locklogin.common.api.packet.CInPacket;
import es.karmadev.locklogin.common.api.packet.COutPacket;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

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
                if (frame.frames() == 1) {
                    byte[] output = new byte[frame.length()];
                    frame.read(output, 0);

                    String raw = new String(Base64.getDecoder().decode(output));
                    IncomingPacket incoming = new CInPacket(raw);

                    if (incoming.getType().equals(DataType.HELLO)) {
                        String whoAmI = incoming.getSequence("channel");

                        if (whoAmI != null) {
                            plugin.getProtocol(null)
                                    .setChannel(whoAmI);

                            OutgoingPacket out = new COutPacket(DataType.HELLO);
                            plugin.getProtocol(null).write(String.format("%s:%s",
                                    StringUtils.generateString(4, StringOptions.LOWERCASE),
                                    StringUtils.generateString(6, StringOptions.LOWERCASE)), out);
                        }
                    }
                }

                plugin.getProtocol(null).receive(identifier, frame);
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