package es.karmadev.locklogin.api.extension.module;

import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.handler.EventHandler;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.module.command.CommandRegistrar;
import es.karmadev.locklogin.api.extension.module.command.error.CommandRuntimeException;
import es.karmadev.locklogin.api.network.NetworkEntity;

import java.util.function.Consumer;

/**
 * LockLogin module manager
 */
@SuppressWarnings("unused")
public interface ModuleManager {

    /**
     * Get the module loader
     *
     * @return the module loader
     */
    ModuleLoader loader();

    /**
     * Fire a new event
     *
     * @param event the event to be fired
     * @throws UnsupportedOperationException if the event doesn't has a getHandlerList method
     */
    void fireEvent(final LockLoginEvent event) throws UnsupportedOperationException;

    /**
     * Add an event handler
     *
     * @param module the module owning the event handler
     * @param handler the event handler
     */
    void addEventHandler(final Module module, final EventHandler handler);

    /**
     * Add an event listener
     *
     * @param module the module owning the handler
     * @param event the event
     * @param listener the event handler
     * @param <T> the event
     * @return the created event handler
     */
    <T extends LockLoginEvent> EventHandler addEventHandler(final Module module, final T event, final Consumer<T> listener);

    /**
     * Execute a command
     *
     * @param issuer the entity command issuer
     * @param command the command
     * @param arguments the command arguments
     * @return if the command was able to be executed
     *
     * @throws CommandRuntimeException if the command fails to execute
     */
    boolean executeCommand(final NetworkEntity issuer, final String command, final String... arguments) throws CommandRuntimeException;

    /**
     * Get the module commands
     *
     * @return the module commands
     */
    CommandRegistrar commands();

    /**
     * Get all the module handlers
     *
     * @param module the module handlers
     * @return the module handlers
     */
    EventHandlerList[] getHandlers(final Module module);
}
