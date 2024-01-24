package es.karmadev.locklogin.common.util.generic;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.communication.packet.listener.NetworkListener;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.NetworkEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a generic plugin listener. This object
 * exists only to make it easier and avoid the "Repeat yourself"
 * on the code.
 */
public final class GenericHandlerList {

    private final static GenericHandlerList GLOBAL = new GenericHandlerList();

    private final Map<Module, Set<ListenerMetaData>> listeners = new ConcurrentHashMap<>();
    private final Set<ListenerMetaData> uncategorized = ConcurrentHashMap.newKeySet();

    /**
     * Do not use any flag
     */
    public final static byte FLAGS_NONE = 8;

    /**
     * The handlers will be available by using the
     * global instance of the handler list
     */
    public final static byte FLAGS_GLOBAL = 16;

    /**
     * The handler will allow null module as
     * a listener owner
     */
    public final static byte FLAGS_NOT_MODULE = 32;

    private final int flags;

    /**
     * Initialize the generic handler list
     */
    public GenericHandlerList() {
        this.flags = FLAGS_NONE;
    }

    /**
     * Initialize the generic handler list
     *
     * @param flags the flags
     */
    public GenericHandlerList(final byte flags) {
        this.flags = flags;
    }

    /**
     * Handle an event
     *
     * @param event the event to handle
     */
    public void handle(final NetworkEvent event) {
        if ((flags & FLAGS_GLOBAL) == FLAGS_GLOBAL) {
            GLOBAL.handleWithFlags(flags ^ FLAGS_GLOBAL, event);
            return;
        }

        handleWithFlags(flags, event);
    }

    /**
     * Handle the event with the flags
     *
     * @param flags the flags
     * @param event the event
     */
    private void handleWithFlags(final int flags, final NetworkEvent event) {
        if ((flags & FLAGS_GLOBAL) == FLAGS_GLOBAL) {
            handleWithFlags(flags ^ FLAGS_GLOBAL, event);
            return;
        }

        if ((flags & FLAGS_NOT_MODULE) == FLAGS_NOT_MODULE) {
            for (ListenerMetaData listener : uncategorized) {
                MethodHandle[] handles = listener.getHandlers(event);
                for (MethodHandle handle : handles) {
                    try {
                        handle.invoke(event);
                    } catch (Throwable ignored) {}
                }
            }
        }

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
     * Add an event listener
     *
     * @param module the module which is registering
     *               the listener
     * @param listener the listener
     */
    public void addListeners(final Module module, final NetworkListener listener) {
        if ((flags & FLAGS_GLOBAL) == FLAGS_GLOBAL) {
            GLOBAL.addListenerWithFlags(flags ^ FLAGS_GLOBAL, module, listener);
            return;
        }

        addListenerWithFlags(flags, module, listener);
    }

    /**
     * Add a listener with the flags
     *
     * @param flags the flags
     * @param module the listener owner
     * @param listener the listener
     */
    private void addListenerWithFlags(final int flags, final Module module, final NetworkListener listener) {
        if ((flags & FLAGS_GLOBAL) == FLAGS_GLOBAL) {
            GLOBAL.addListenerWithFlags(flags ^ FLAGS_GLOBAL, module, listener);
            return;
        }

        if (module == null && (flags & FLAGS_NOT_MODULE) != FLAGS_NOT_MODULE) {
            throw new NullPointerException();
        }

        if (module == null) {
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
            uncategorized.add(metaData);
            return;
        }

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
     * Remove a single listener from the
     * handler list
     *
     * @param listener the listener to remove
     */
    public void removeListener(final NetworkListener listener) {
        if ((flags & FLAGS_GLOBAL) == FLAGS_GLOBAL) {
            removeListenerWithFlags(flags ^ FLAGS_GLOBAL, listener);
            return;
        }

        removeListenerWithFlags(flags, listener);
    }

    /**
     * Remove a listener with flags
     *
     * @param flags the flags
     * @param listener the listener to remove
     */
    private void removeListenerWithFlags(final int flags, final NetworkListener listener) {
        if ((flags & FLAGS_GLOBAL) == FLAGS_GLOBAL) {
            removeListenerWithFlags(flags ^ FLAGS_GLOBAL, listener);
            return;
        }

        if ((flags & FLAGS_NOT_MODULE) == FLAGS_NOT_MODULE) {
            uncategorized.removeIf((meta) -> meta.getListener().equals(listener));
        }

        for (Module module : listeners.keySet()) {
            Set<ListenerMetaData> listenerSet = listeners.get(module);
            listenerSet.removeIf(meta -> meta.getListener().equals(listener));
        }
    }

    /**
     * Remove all the listeners registered by
     * the module
     *
     * @param module the module which registered
     *               the listeners
     */
    public void removeListeners(final Module module) {
        if (module == null) throw new NullPointerException();
        listeners.remove(module);
    }
}
