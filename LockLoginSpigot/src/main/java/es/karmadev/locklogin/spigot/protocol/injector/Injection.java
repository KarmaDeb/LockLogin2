package es.karmadev.locklogin.spigot.protocol.injector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin client injection
 */
public class Injection extends ChannelDuplexHandler {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
    static PublicKey sharedPublic;

    private UUID client;
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
            }

            Object identifierObject = getField(packet, identifierField);
            Object dataObject = getField(packet, dataField);

            assert identifierObject != null && dataObject != null;

            String identifier = identifierObject.toString();
            //if (!identifier.equals("test:test")) return;

            byte[] data;
            try {
                ByteBuf buf = (ByteBuf) dataObject;
                data = new byte[buf.readableBytes()];
                buf.readBytes(data);
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
                        //plugin.info(ex.fillInStackTrace().toString());
                    }

                    return rawPacket;
                }));

                /*byte[] encrypted = new byte[frame.length()];
                frame.read(encrypted, 0);*/

                builder.append(frame);

                int position = frame.position();
                int max = frame.frames();

                //plugin.info("Frame {0} encrypted is: {1}", position, new String(encrypted));

                if (position == max) {
                    try {
                        byte[] rawPacket = builder.build();

                        String raw = new String(rawPacket, StandardCharsets.UTF_8);
                        Object comPacket = StringUtils.load(raw).orElse(null);

                        if (comPacket instanceof OutgoingPacket) {
                            OutgoingPacket out = (OutgoingPacket) comPacket;
                            JsonObject outBuild = out.build();

                            outBuild.addProperty("identifier", identifier);
                            outBuild.addProperty("replying", out.id());

                            Gson gson = new GsonBuilder().create();
                            raw = gson.toJson(outBuild);

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

                            Gson gson = new GsonBuilder().create();
                            JsonObject obj = gson.fromJson(new String(inData, StandardCharsets.UTF_8), JsonObject.class);
                            obj.addProperty("identifier", identifier);
                            obj.addProperty("replying", incoming.id());

                            String rawJson = gson.toJson(obj);
                            incoming = new CInPacket(rawJson);

                            //plugin.onReceive(incoming);
                            NetworkChannel ch = plugin.getChannel(identifier);

                            if (ch == null) {
                                throw new IllegalStateException("Received a packet from unregistered channel: " + identifier);
                            }

                            ch.handle(
                                    new PacketReceiveEvent(ch, incoming)
                            );
                        }
                    } catch (InvalidPacketDataException ex) {
                        plugin.log(ex, "Failed to handle packet under tag {0}", identifier);
                    }
                }
            }

            if (object instanceof OutgoingPacket) {
                OutgoingPacket out = (COutPacket) object;
                JsonObject outBuild = out.build();

                if (out.getType().equals(DataType.HELLO)) {
                    out.addProperty("identifier", identifier);
                    byte[] rawKey = Base64.getDecoder().decode(outBuild.get("key").getAsString());
                    KeyFactory factory = KeyFactory.getInstance("RSA");
                    EncodedKeySpec keySpec = new X509EncodedKeySpec(rawKey);
                    sharedPublic = factory.generatePublic(keySpec);

                    outBuild.addProperty("identifier", identifier);
                    outBuild.addProperty("replying", out.id());

                    Gson gson = new GsonBuilder().create();
                    String raw = gson.toJson(outBuild);

                    IncomingPacket converted = new CInPacket(raw);
                    NetworkChannel ch = plugin.getChannel(identifier);

                    if (ch == null) {
                        throw new IllegalStateException("Received a packet from unregistered channel: " + identifier);
                    }

                    ch.handle(
                            new PacketReceiveEvent(ch, converted)
                    );
                    //plugin.onReceive(converted);
                }
            }
        }

        super.channelRead(context, packet);
    }

    protected void inject(final Player player) {
        if (!injected) {
            if (player == null) return;

            this.client = player.getUniqueId();

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

            this.client = null;
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
        Class<?> playerConnection = SpigotServer.netMinecraftServer("PlayerConnection").orElseGet(() ->
                SpigotServer.netMinecraftServer("PlayerConnection", "network").orElse(null)
        );
        if (playerConnection == null) return null;

        Field[] fields = entityHandle.getClass().getDeclaredFields();
        Field targetField = null;
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            if (fieldType.equals(playerConnection)) {
                targetField = field;
                break;
            }
        }
        if (targetField == null) return null;

        try {
            targetField.setAccessible(true);
            return targetField.get(entityHandle);
        } catch (IllegalAccessException | SecurityException ex) {
            return null;
        }
    }

    private Object networkManager(final Object playerConnection) {
        Class<?> networkManager = SpigotServer.netMinecraftServer("NetworkManager").orElseGet(() -> {
            try {
                return Class.forName("net.minecraft.network.NetworkManager");
            } catch (ClassNotFoundException ex) {
                return null;
            }
        });
        if (networkManager == null) return null;

        Field[] fields = playerConnection.getClass().getDeclaredFields();
        Field targetField = null;
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            if (fieldType.equals(networkManager)) {
                targetField = field;
                break;
            }
        }
        if (targetField == null) return null;

        try {
            targetField.setAccessible(true);
            return targetField.get(playerConnection);
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

        Class<Channel> channel = Channel.class;
        Field[] fields = networkManager.getClass().getDeclaredFields();
        Field targetField = null;
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            if (fieldType.equals(channel)) {
                targetField = field;
                break;
            }
        }
        if (targetField == null) return null;

        try {
            targetField.setAccessible(true);
            return (Channel) targetField.get(networkManager);
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

    private Object invokeMethod(final Object object, final String method) {
        try {
            Method m = object.getClass().getDeclaredMethod(method);
            m.setAccessible(true);

            return m.invoke(object);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | SecurityException ex) {
            return null;
        }
    }

    private Object invokeMethod(final Object object, final String method, final Class<?>[] paramTypes, final Object[] params) {
        try {
            Method m = object.getClass().getDeclaredMethod(method, paramTypes);
            m.setAccessible(true);

            return m.invoke(object, params);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | SecurityException ex) {
            return null;
        }
    }
}