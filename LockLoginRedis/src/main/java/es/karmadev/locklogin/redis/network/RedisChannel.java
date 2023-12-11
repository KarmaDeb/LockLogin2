package es.karmadev.locklogin.redis.network;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import es.karmadev.locklogin.api.network.communication.packet.listener.NetworkListener;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.NetworkEvent;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannelQue;
import lombok.Getter;
import lombok.Value;
import redis.clients.jedis.JedisCluster;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RedisChannel implements NetworkChannel {

    private final String channel;
    private final RedisMessageQue que;

    private final Map<Module, Set<ListenerMetaData>> listeners = new ConcurrentHashMap<>();

    public RedisChannel(final JedisCluster cluster, final String channel) {
        this.channel = channel;
        this.que = new RedisMessageQue(cluster, this);
    }

    /**
     * Get the channel name this
     * network channel works at
     *
     * @return the channel name
     */
    @Override
    public String getChannel() {
        return channel;
    }

    /**
     * Get the channel que
     *
     * @return the channel que
     */
    @Override
    public NetworkChannelQue getProcessingQue() {
        return que;
    }

    /**
     * Handle a network event
     *
     * @param event the event to handle
     */
    @Override
    public void handle(final NetworkEvent event) {
        for (Module module : listeners.keySet()) {
            Set<ListenerMetaData> meta = listeners.get(module);
            for (ListenerMetaData listener : meta) {
                MethodHandle[] handles = listener.getHandlers(event);
                for (MethodHandle handle : handles) {
                    try {
                        handle.invoke(event);
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    /**
     * Add a listener
     *
     * @param module   the module listener owner
     * @param listener the listener
     */
    @Override
    public void addListener(final Module module, final NetworkListener listener) {
        Set<ListenerMetaData> meta = listeners.computeIfAbsent(module, k -> ConcurrentHashMap.newKeySet());

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<? extends NetworkListener> listenerClass = listener.getClass();

        Map<Class<? extends NetworkEvent>, Set<MethodHandle>> handles = new HashMap<>();
        for (Method method : listenerClass.getDeclaredMethods()) {
            if (!method.getReturnType().equals(Void.class)) {
                continue;
            }
            Parameter[] parameters = method.getParameters();
            if (parameters.length != 1) {
                continue;
            }

            Parameter firstParameter = parameters[0];
            Class<?> firstType = firstParameter.getType();

            if (!NetworkEvent.class.isAssignableFrom(firstType)) {
                continue;
            }

            Class<? extends NetworkEvent> eventClass = firstType.asSubclass(NetworkEvent.class);
            int modifiers = eventClass.getModifiers();

            if (Modifier.isAbstract(modifiers) ||
                    Modifier.isInterface(modifiers) ||
                    Modifier.isPrivate(method.getModifiers()) ||
                    Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            try {
                Set<MethodHandle> handleList = handles.computeIfAbsent(eventClass, k -> new HashSet<>());
                MethodHandle handle = lookup.unreflect(method)
                        .bindTo(listenerClass)
                        .asType(MethodType.methodType(void.class));

                handleList.add(handle);
            } catch (IllegalAccessException ignored) {}
        }

        ListenerMetaData metaData = ListenerMetaData.of(listener);
        metaData.getInvokers().putAll(handles);
        meta.add(metaData);
    }

    /**
     * Remove a single listener
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(final NetworkListener listener) {
        for (Module module : listeners.keySet()) {
            Set<ListenerMetaData> listenerSet = listeners.get(module);
            listenerSet.removeIf(meta -> meta.getListener().equals(listener));
        }
    }

    /**
     * Remove all the listeners registered
     * on the module
     *
     * @param module the module to remove listeners
     *               from
     */
    @Override
    public void removeListeners(final Module module) {
        listeners.remove(module);
    }
}

