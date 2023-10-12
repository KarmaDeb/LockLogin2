package es.karmadev.locklogin.common.plugin.secure.logger;

import es.karmadev.api.core.source.APISource;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.Message;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the plugin log filter,
 * applied to the application core logger
 * to hide sensitive information from server
 * administrators. If you are a user reviewing the
 * plugin source code, then please note, some servers
 * might be running a self-built version of the plugin
 * without the filter, the fact that this exists here doesn't
 * mean a suspicious server using LockLogin is unable to see
 * your passwords. If you want to see more features
 * we implement to protect your data, refer to {@link CommandMask}
 */
@SuppressWarnings("unused")
public class Log4Logger implements Filter {

    private static boolean enabled = true;
    private static final Set<String> sensitiveCommands = ConcurrentHashMap.newKeySet();

    private final APISource source;

    /**
     * Initialize the filter
     *
     * @param sensitive the set of sensitive commands
     */
    public Log4Logger(final APISource source, final Set<String> sensitive) {
        this.source = source;
        sensitiveCommands.addAll(sensitive);
        enabled = true;
    }

    /**
     * Add a sensitive command to
     * the list of sensitive commands
     *
     * @param command the command to add
     */
    public static void addSensitiveCommand(final String command) {
        sensitiveCommands.add(command);
    }

    /**
     * Get if the message is a sensitive
     * message
     *
     * @param message the message to check
     * @return if the message is a sensitive
     * message
     */
    private static boolean isSensitive(final String message) {
        if (ObjectUtils.isNullOrEmpty(message)) return false;
        String data = message.toLowerCase();

        return (data.contains("executed command") || data.contains("issued server command") ||
                data.contains("issued command") || data.contains("ran command")) && sensitiveCommands.stream().anyMatch(message::contains);
    }

    /**
     * Returns the result that should be returned when the filter does not match the event.
     *
     * @return the Result that should be returned when the filter does not match the event.
     */
    @Override
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }

    /**
     * Returns the result that should be returned when the filter matches the event.
     *
     * @return the Result that should be returned when the filter matches the event.
     */
    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level  The event logging Level.
     * @param marker The Marker for the event or null.
     * @param msg    String text to filter on.
     * @param params An array of parameters or null.
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        if (!enabled) return Result.NEUTRAL;
        return isSensitive(msg) ? Result.DENY : Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @param p2      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @param p2      the message parameters
     * @param p3      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @param p2      the message parameters
     * @param p3      the message parameters
     * @param p4      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @param p2      the message parameters
     * @param p3      the message parameters
     * @param p4      the message parameters
     * @param p5      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @param p2      the message parameters
     * @param p3      the message parameters
     * @param p4      the message parameters
     * @param p5      the message parameters
     * @param p6      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @param p2      the message parameters
     * @param p3      the message parameters
     * @param p4      the message parameters
     * @param p5      the message parameters
     * @param p6      the message parameters
     * @param p7      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @param p2      the message parameters
     * @param p3      the message parameters
     * @param p4      the message parameters
     * @param p5      the message parameters
     * @param p6      the message parameters
     * @param p7      the message parameters
     * @param p8      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger  The Logger.
     * @param level   the event logging level.
     * @param marker  The Marker for the event or null.
     * @param message The message.
     * @param p0      the message parameters
     * @param p1      the message parameters
     * @param p2      the message parameters
     * @param p3      the message parameters
     * @param p4      the message parameters
     * @param p5      the message parameters
     * @param p6      the message parameters
     * @param p7      the message parameters
     * @param p8      the message parameters
     * @param p9      the message parameters
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        if (!enabled) return Result.NEUTRAL;
        if (isSensitive(message)) {
            return Result.DENY;
        }

        return Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level  The event logging Level.
     * @param marker The Marker for the event or null.
     * @param msg    Any Object.
     * @param t      A Throwable or null.
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        if (!enabled) return Result.NEUTRAL;

        if (msg instanceof Throwable) return Result.NEUTRAL;
        return isSensitive(msg.toString()) ? Result.DENY : Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param logger The Logger.
     * @param level  The event logging Level.
     * @param marker The Marker for the event or null.
     * @param msg    The Message
     * @param t      A Throwable or null.
     * @return the Result.
     */
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        if (!enabled) return Result.NEUTRAL;
        return msg != null && isSensitive(msg.getFormattedMessage()) ? Result.DENY : Result.NEUTRAL;
    }

    /**
     * Filter an event.
     *
     * @param event The Event to filter on.
     * @return the Result.
     */
    @Override
    public Result filter(LogEvent event) {
        if (!enabled) return Result.NEUTRAL;

        if (event != null) {
            Message m = event.getMessage();
            if (m != null) {
                String formatted = m.getFormattedMessage();

                if (isSensitive(formatted)) {
                    if (m instanceof MutableLogEvent) {
                        MutableLogEvent message = (MutableLogEvent) m;

                        if (formatted.contains(" ")) {
                            String[] fData = formatted.split(" ");
                            String w = null;
                            for (String word : fData) {
                                if (CommandMask.mustMask(word)) {
                                    w = word;
                                    break;
                                }
                            }

                            if (w != null) {
                                int index = formatted.indexOf(w);

                                String commandC = formatted.substring(index);
                                String command = commandC;
                                String[] args = new String[0];
                                if (command.contains(" ")) {
                                    String[] argsData = command.split(" ");

                                    command = argsData[0];
                                    args = new String[argsData.length - 1];
                                    System.arraycopy(argsData, 1, args, 0, args.length);

                                    String lastArgument = args[args.length - 1];
                                    try {
                                        //noinspection ResultOfMethodCallIgnored
                                        UUID.fromString(lastArgument);
                                        return Result.ACCEPT; //Already masked, allow
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }

                                String masked = CommandMask.maksSilent(command, args);
                                try {
                                    Field messageTextField = message.getClass().getDeclaredField("messageText");
                                    messageTextField.setAccessible(true);

                                    StringBuilder builder = new StringBuilder(formatted.replace(commandC, masked));
                                    messageTextField.set(message, builder);

                                    return Result.ACCEPT;
                                } catch (NoSuchFieldException | IllegalAccessException ex) {
                                    return Result.DENY; //Fallback to legacy method to prevent logging
                                }
                            }
                        }
                    }

                    return Result.DENY; //Deny at first instance
                }
            }
        }

        return Result.NEUTRAL;
    }

    /**
     * Gets the life-cycle state.
     *
     * @return the life-cycle state
     */
    @Override
    public State getState() {
        return (enabled ? State.STARTED : State.STOPPED);
    }

    @Override
    public void initialize() {

    }

    @Override
    public void start() {
        enabled = true;
        source.logger().send(LogLevel.SUCCESS, "LockLogin logger has been marked as running");
    }

    @Override
    public void stop() {
        enabled = false;
        source.logger().send(LogLevel.SEVERE, "LockLogin logger has been marked as stopped, if you are not shutting down your server, this might be a malicious action!");
    }

    @Override
    public boolean isStarted() {
        return enabled;
    }

    @Override
    public boolean isStopped() {
        return !enabled;
    }
}
