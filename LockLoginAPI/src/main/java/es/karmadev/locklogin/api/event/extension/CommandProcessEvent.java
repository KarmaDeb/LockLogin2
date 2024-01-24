package es.karmadev.locklogin.api.event.extension;

import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.NetworkEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * When a command is processed
 */
public class CommandProcessEvent extends LockLoginEvent implements CommandEvent {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    private final NetworkEntity issuer;

    private String cmd;
    private String command;
    private String[] arguments;
    private boolean cancelled = false;
    private String reason = "";

    /**
     * Initialize the command event
     *
     * @param entity the entity
     * @param cmd the command
     * @param command the command name
     * @param arguments the command arguments
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent()}
     */
    public CommandProcessEvent(final NetworkEntity entity, final String cmd, final String command, final String... arguments) throws SecurityException {
        this(null, entity, cmd, command, arguments);
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @param cmd the command
     * @param command the command name
     * @param arguments the command arguments
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent(Module)}
     */
    public CommandProcessEvent(final Module caller, final NetworkEntity entity, final String cmd, final String command, final String... arguments) throws SecurityException {
        super(caller);
        this.issuer = entity;

        this.cmd = cmd;
        this.command = command;
        this.arguments = arguments;
    }

    /**
     * Cancel the event
     *
     * @param cancel if the event is cancelled
     * @param reason the cancel reason
     */
    @Override
    public void setCancelled(final boolean cancel, final String reason) {
        cancelled = cancel;
        this.reason = reason;
    }

    /**
     * Get if the event is cancelled
     *
     * @return if the event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Get the event cancel reason
     *
     * @return the cancel reason
     */
    @Override
    public String cancelReason() {
        return reason;
    }

    /**
     * Get the command issuer
     *
     * @return the command issuer
     */
    public NetworkEntity getIssuer() {
        return issuer;
    }

    /**
     * Get the command message
     *
     * @return the command message
     */
    @Override
    public String getMessage() {
        return cmd;
    }

    /**
     * Get the command
     *
     * @return the command
     */
    @Override
    public String getCommand() {
        return command;
    }

    /**
     * Get the command arguments
     *
     * @return the arguments
     */
    @Override
    public String[] getArguments() {
        return arguments.clone();
    }

    /**
     * Set the command
     *
     * @param command the new command
     */
    @Override
    public void setCommand(final String command) {
        cmd = command;
        this.command = getLabel(cmd);
        arguments = parseArguments(cmd);
    }

    private String getLabel(final String command) {
        String parseTarget = command;
        if (command.contains(" ")) {
            String[] data = command.split(" ");
            parseTarget = data[0];
        }

        if (parseTarget.contains(":")) {
            String[] data = parseTarget.split(":");
            String plugin = data[0];
            parseTarget = parseTarget.replaceFirst(plugin + ":", "");
        }

        return parseTarget;
    }

    private String[] parseArguments(final String command) {
        List<String> arguments = new ArrayList<>();
        if (command.contains(" ")) {
            String[] data = command.split(" ");
            if (data.length >= 1) {
                arguments.addAll(Arrays.asList(Arrays.copyOfRange(data, 1, data.length)));
            }
        }

        return arguments.toArray(new String[0]);
    }

    /**
     * Get all the handlers for this
     * event
     *
     * @return the event handlers
     */
    public static EventHandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
