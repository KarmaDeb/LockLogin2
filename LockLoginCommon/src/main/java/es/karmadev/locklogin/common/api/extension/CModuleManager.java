package es.karmadev.locklogin.common.api.extension;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.extension.CommandProcessEvent;
import es.karmadev.locklogin.api.event.handler.EventHandler;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.event.handler.Listener;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.ModuleLoader;
import es.karmadev.locklogin.api.extension.module.ModuleManager;
import es.karmadev.locklogin.api.extension.module.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.module.command.error.CommandRuntimeException;
import es.karmadev.locklogin.api.extension.module.command.worker.CommandExecutor;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.TextContainer;
import es.karmadev.locklogin.common.api.extension.command.CCommandMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class CModuleManager implements ModuleManager {

    private final ModuleLoader loader = new ModuleLoader(this);
    private final CCommandMap commands = new CCommandMap(this);
    private final Map<Module, Set<Class<? extends LockLoginEvent>>> module_events = new ConcurrentHashMap<>();

    public Function<ModuleCommand, Boolean> onCommandRegistered;
    public Consumer<ModuleCommand> onCommandUnregistered;

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
            if (!module.isEnabled()) continue;
            EventHandler[] handlers = list.getHandlers(module);

            for (EventHandler handler : handlers) {
                Method[] methods = handler.getClass().getDeclaredMethods();
                for (Method method : methods) {
                    Parameter[] parameters = method.getParameters();
                    if (parameters.length == 1) {
                        Parameter parameter = parameters[0];
                        Class<?> paramType = parameter.getType();

                        if (event.getClass().isAssignableFrom(paramType)) {
                            method.setAccessible(true);
                            try {
                                method.invoke(handler, event);
                            } catch (IllegalAccessException | InvocationTargetException ex) {
                                plugin.log(ex, "An exception has raised while trying to fire event " + event.getClass().getSimpleName());
                                //ex.printStackTrace();
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
     * @param module the module owning the event handler
     * @param handler the event handler
     */
    @Override
    public void addEventHandler(final Module module, final EventHandler handler) {
        Method[] methods = handler.getClass().getDeclaredMethods();

        if (module != null && module.isEnabled()) {
            for (Method method : methods) {
                if (!method.isAnnotationPresent(Listener.class)) continue;

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
     * @param module the module owning the handler
     * @param event    the event
     * @param listener the event handler
     * @param <T> the event type
     * @return the event handler
     */
    @Override
    public <T extends LockLoginEvent> EventHandler addEventHandler(final Module module, final T event, final Consumer<T> listener) {

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

    /**
     * Execute a command
     *
     * @param issuer  the entity command issuer
     * @param command the command
     * @param arguments the command arguments
     * @return if the command was able to be executed
     *
     * @throws CommandRuntimeException if the command fails to execute
     */
    @Override
    public boolean executeCommand(final NetworkEntity issuer, final String command, final String... arguments) throws CommandRuntimeException {
        Map<Module, Map<ModuleCommand, Set<String>>> registeredCommands = commands.commands;
        String targetCommand = command;
        if (targetCommand.startsWith("/")) {
            targetCommand = command.substring(1);
        }

        for (Module module : registeredCommands.keySet()) {
            if (!module.isEnabled()) continue;

            Map<ModuleCommand, Set<String>> commandMap = registeredCommands.get(module);
            if (commandMap == null || commandMap.isEmpty()) continue;

            for (ModuleCommand cmd : commandMap.keySet()) {
                Set<String> keys = commandMap.get(cmd);

                for (String cmdName : keys) {
                    if (cmdName.equalsIgnoreCase(targetCommand)) {
                        CommandExecutor executor = cmd.getExecutor();
                        Module owner = cmd.getModule();

                        List<String> stringArguments = new ArrayList<>();
                        for (String argument : arguments)
                            if (argument != null && !ObjectUtils.isNullOrEmpty(argument))
                                stringArguments.add(argument);

                        try {
                            if (executor != null) {
                                StringBuilder commandBuilder = new StringBuilder("/").append(command);
                                for (int i = 0; i < arguments.length; i++) {
                                    commandBuilder.append(arguments[i]);
                                    if (i != arguments.length - 1) commandBuilder.append(" ");
                                }

                                CommandProcessEvent event = new CommandProcessEvent(issuer, commandBuilder.toString(), command, stringArguments.toArray(new String[0]));
                                fireEvent(event);

                                if (!event.isCancelled()) {
                                    String finalCommand = event.getCommand();
                                    String[] finalArguments = event.getArguments();

                                    executor.execute(issuer, finalCommand, finalArguments);
                                    return true;
                                } else {
                                    if (issuer instanceof TextContainer) {
                                        TextContainer container = (TextContainer) issuer;
                                        container.sendMessage("&cFailed to issue command " + event.getMessage() + ". &7" + event.cancelReason());
                                    }
                                }
                            }
                        } catch (Throwable ex) {
                            throw new CommandRuntimeException(ex, "Blame author(s) of module " + owner.getDescription().getName() + " (" + owner.getDescription().getAuthor() + ") for this exception. THIS IS NOT CAUSED BY LOCKLOGIN BUT ONE OF ITS MODULES");
                        }
                    }
                }
            }
        }

        return false;
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
    @Override
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
