package es.karmadev.locklogin.spigot.protocol.injector;

import es.karmadev.api.spigot.server.SpigotServer;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import io.netty.channel.*;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

/**
 * LockLogin client injection
 */
public class Injection extends ChannelDuplexHandler {

    private final LockLogin plugin = CurrentPlugin.getPlugin();

    @Getter
    private boolean injected = false;
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
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                Class<?> fieldType = field.getType();
                String ftName = fieldType.getSimpleName();

                if (ftName.equalsIgnoreCase("String") || ftName.toLowerCase().contains("key")) {
                    identifierField = field.getName();
                }
                if (ftName.contains("byte") || ftName.equalsIgnoreCase("PacketDataSerializer")) {
                    dataField = field.getName();
                }
            }

            if (identifierField == null || dataField == null) {
                System.out.println("Unable to identify");
            }

            Object identifierObject = getField(packet, identifierField);
            Object dataObject = getField(packet, dataField);

            if (identifierObject == null || dataObject == null) {
                System.out.println("Unable to read");
            }
            assert identifierObject != null && dataObject != null;

            String identifier = identifierObject.toString();
            byte[] data;
            try {
                data = (byte[]) dataObject;
            } catch (ClassCastException ex) {
                int readAbleBytes = (int) invokeMethod(dataObject, "readableBytes");
                data = new byte[readAbleBytes];

                invokeMethod(dataObject, "readBytes", new Class[]{byte[].class}, new Object[]{data});
                //nmsData.invokeMethodForNmsObject("readBytes", new Class[]{byte[].class}, new Object[]{data});
            }

            String rawData = new String(data, StandardCharsets.UTF_8);
            //TODO: Make identifier to be shared-generated
        }

        super.channelRead(context, packet);
    }

    protected void inject(final Player player) {
        if (!injected) {
            if (player == null) return;

            this.channel = getChannel(player);
            if (channel == null) return;

            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addBefore("packet_handler", "LockLogin:" + player.getName(), this);
            injected = true;

            plugin.info("Successfully injected into {0}", player.getName());
        }
    }

    protected void release() {
        if (injected) {
            if (channel != null) {
                EventLoop loop = channel.eventLoop();
                loop.submit(() -> channel.pipeline().remove(this));
            }

            plugin.info("Successfully released");
            injected = false;
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
        Object entityHandle = toEntityHandle(player);
        if (entityHandle == null) return null;

        Object playerConnection = playerConnection(entityHandle);
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