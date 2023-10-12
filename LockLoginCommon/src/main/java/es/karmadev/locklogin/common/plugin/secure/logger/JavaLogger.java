package es.karmadev.locklogin.common.plugin.secure.logger;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

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
public class JavaLogger implements Filter {

    private static final Set<String> sensitiveCommands = ConcurrentHashMap.newKeySet();

    /**
     * Initialize the filter
     *
     * @param sensitive the set of sensitive commands
     */
    public JavaLogger(final Set<String> sensitive) {
        sensitiveCommands.addAll(sensitive);
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
     * Check if a given log record should be published.
     *
     * @param record a LogRecord
     * @return true if the log record should be published.
     */
    @Override
    public boolean isLoggable(final LogRecord record) {
        if (record == null) return true;
        String message = record.getMessage();
        if (ObjectUtils.isNullOrEmpty(message)) return true;

        if (isSensitive(record.getMessage())) {
            if (message.contains(" ")) {
                String[] fData = message.split(" ");
                String w = null;
                for (String word : fData) {
                    if (CommandMask.mustMask(word)) {
                        w = word;
                        break;
                    }
                }

                if (w != null) {
                    int index = message.indexOf(w);

                    String commandC = message.substring(index);
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
                            return true; //Already masked, do nothing
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    String masked = CommandMask.maksSilent(command, args);
                    record.setMessage(message.replace(commandC, masked));

                    return true;
                }
            }

            return false; //Fail by default
        }

        return true;
    }
}
