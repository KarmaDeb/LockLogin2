package es.karmadev.locklogin.common.api.extension;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.handler.EventHandler;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.extension.manager.ModuleLoader;
import es.karmadev.locklogin.api.extension.manager.ModuleManager;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.common.api.extension.command.CCommandMap;
import es.karmadev.locklogin.common.api.extension.loader.CModuleLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CModuleManager implements ModuleManager {

    private final CModuleLoader loader = new CModuleLoader(this);
    private final CCommandMap commands = new CCommandMap(this);
    private final Map<Module, Set<Class<? extends LockLoginEvent>>> module_events = new ConcurrentHashMap<>();

    /**
     * Get the module loader
     *
     * @return the module loader
     */
    @Override
    public ModuleLoader loader() {
        return loader;
    }

    /**
     * Fire a new event
     *
     * @param event the event to be fired
     * @throws UnsupportedOperationException if the event doesn't has a getHandlerList method
     */
    @Override
    public void fireEvent(final LockLoginEvent event) throws UnsupportedOperationException {
        EventHandlerList list = getHandlerList(event.getClass());
        LockLogin plugin = CurrentPlugin.getPlugin();

        for (Module module : loader.getModules()) {
            plugin.info(module.toString());
            EventHandler[] handlers = list.getHandlers(module);

            for (EventHandler handler : handlers) {
                Method[] methods = handler.getClass().getDeclaredMethods();
                for (Method method : methods) {
                    Parameter[] parameters = method.getParameters();
                    if (parameters.length == 1) {
                        Parameter parameter = parameters[0];
                        Class<?> paramType = parameter.getType();

                        if (paramType.equals(event.getClass())) {
                            method.setAccessible(true);
                            try {
                                method.invoke(handler, event);
                            } catch (IllegalAccessException | InvocationTargetException ex) {
                                plugin.log(ex, "An exception has raised while trying to fire event " + event.getClass().getSimpleName());
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Add an event handler
     *
     * @param handler the event handler
     */
    @Override
    public void addEventHandler(final EventHandler handler) {
        Method[] methods = handler.getClass().getDeclaredMethods();
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.runtime();
        Path caller = runtime.caller();

        Module module = loader.findByFile(caller);
        if (module != null) {
            for (Method method : methods) {
                Parameter[] parameters = method.getParameters();
                if (parameters.length == 1) {
                    Parameter parameter = parameters[0];
                    Class<?> paramType = parameter.getType();
                    Class<LockLoginEvent> eventClass = LockLoginEvent.class;

                    if (eventClass.isAssignableFrom(paramType)) {
                        Class<? extends LockLoginEvent> sub = paramType.asSubclass(eventClass);
                        EventHandlerList handlerList = getHandlerList(sub);

                        handlerList.addHandler(handler, module);
                        Set<Class<? extends LockLoginEvent>> hooked = module_events.getOrDefault(module, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                        hooked.add(sub);

                        module_events.put(module, hooked);
                    }
                }
            }
        }
    }

    /**
     * Add an event listener
     *
     * @param event    the event
     * @param listener the event handler
     */
    @Override
    public <T extends LockLoginEvent> EventHandler addEventHandler(final T event, final Consumer<T> listener) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.runtime();
        Path caller = runtime.caller();

        Module module = loader.findByFile(caller);
        if (module != null) {
            EventHandler handler = new EventHandler() {

                @SuppressWarnings("unused")
                public void onUnknown(final T event) {
                    listener.accept(event);
                }
            };
            EventHandlerList handlerList = getHandlerList(event.getClass());
            handlerList.addHandler(handler, module);

            Set<Class<? extends LockLoginEvent>> hooked = module_events.getOrDefault(module, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            hooked.add(event.getClass());

            module_events.put(module, hooked);

            return handler;
        }

        return null;
    }

    /**
     * Execute a command
     *
     * @param issuer  the entity command issuer
     * @param command the command
     */
    @Override
    public void executeCommand(final NetworkEntity issuer, final String command) {

    }

    /**
     * Get the module commands
     *
     * @return the module commands
     */
    @Override
    public CCommandMap commands() {
        return commands;
    }

    /**
     * Get all the module handlers
     *
     * @param module the module handlers
     * @return the module handlers
     */
    public EventHandlerList[] getHandlers(final Module module) {
        List<EventHandlerList> list = new ArrayList<>();
        Set<Class<? extends LockLoginEvent>> hooked = module_events.getOrDefault(module, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        for (Class<? extends LockLoginEvent> clazz : hooked) {
            try {
                EventHandlerList handlerList = getHandlerList(clazz);
                list.add(handlerList);
            } catch (UnsupportedOperationException ignored) {}
        }

        return list.toArray(new EventHandlerList[0]);
    }

    private EventHandlerList getHandlerList(final Class<? extends LockLoginEvent> event) throws UnsupportedOperationException {
        try {
            Method getHandlers = event.getDeclaredMethod("getHandlers");
            EventHandlerList list = (EventHandlerList) getHandlers.invoke(event);

            if (list == null) throw new UnsupportedOperationException("Cannot handle event " + event.getSimpleName() + " as a method getHandlers is required but was not found");

            return list;
        } catch (NoSuchMethodException | ClassCastException | IllegalAccessException | InvocationTargetException e) {
            throw new UnsupportedOperationException("Cannot handle event " + event.getSimpleName() + " as a method getHandlers is required but was not found");
        }
    }
}
