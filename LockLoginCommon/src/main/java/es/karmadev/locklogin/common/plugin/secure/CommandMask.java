package es.karmadev.locklogin.common.plugin.secure;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;

/**
 * LockLogin CommandMask, this class's only objective
 * is to hide sensitive information from the command
 * and apply a "mask" over it, so plugins of type chat-spy
 * cannot see the data from the command. In a normal environment,
 * LockLogin is the only one able to read data from here, but
 * some server owners may infringe those features in order to
 * steal your passwords. Please always verify in which server you
 * are playing
 */
public class CommandMask {

    private final static String[] filter = new String[]{
            "register",
            "login",
            "pin",
            "totp",
            "staff"
    };

    private final static String[] subArguments = new String[]{
            "change",
            "remove",
            "setup",
            "enable",
            "disable",
            "toggle",
            "register",
            "login",
            "logout",
            "unregister",
            "unban",
            "lookup"
    };

    private final static Map<UUID, String[]> args = new ConcurrentHashMap<>();
    private final static Map<UUID, String> commands = new ConcurrentHashMap<>();

    /**
     * Get if the command should be masked
     *
     * @param cmd the command
     * @return if the command should be masked
     */
    public static boolean mustMask(final String cmd) {
        String cmdName = cmd.toLowerCase();
        if (cmdName.startsWith("/")) cmdName = cmdName.substring(1);

        if (cmdName.contains(":")) {
            String pluginName;

            if (cmdName.contains(" ")) {
                String[] preData = cmdName.split(" ");
                String origin = preData[0];

                if (origin.contains(":")) {
                    String[] cmdData = cmd.split(":");
                    pluginName = cmdData[0];

                    cmdName = cmd.replaceFirst(pluginName + ":", "");
                }
            } else {
                String[] cmdData = cmd.split(":");
                pluginName = cmdData[0];

                try {
                    cmdName = cmd.replaceFirst(pluginName + ":", "");
                } catch (PatternSyntaxException ignored) {}
            }
        }

        String match = cmdName;
        if (match.contains(" ")) {
            String[] data = cmdName.split(" ");
            match = data[0];
        }

        return Arrays.stream(filter).anyMatch(match::equalsIgnoreCase);
    }

    /**
     * Mask a command
     *
     * @param arguments the arguments
     * @return the mask ID
     */
    public static UUID mask(final String cmd, final String... arguments) {
        UUID id = UUID.randomUUID();
        String command = cmd;
        for (String arg : arguments) {
            if (Arrays.stream(subArguments).noneMatch(arg::equalsIgnoreCase)) {
                command = command.replace(arg, hide(arg));
            }
        }

        args.put(id, arguments);
        commands.put(id, command);
        return id;
    }

    /**
     * Mask a command silently, without storing
     * any information about it
     *
     * @param cmd the command
     * @param arguments the command arguments
     * @return the masked command
     */
    public static String maksSilent(final String cmd, final String... arguments) {
        StringBuilder builder = new StringBuilder(cmd).append(" ");
        int index = 0;
        for (String arg : arguments) {
            if (Arrays.stream(subArguments).noneMatch(arg::equalsIgnoreCase)) {
                String masked = hide(arg);
                builder.append(masked);
                if (index++ != arguments.length - 1) {
                    builder.append(" ");
                }
            }
        }

        return builder.toString();
    }

    /**
     * Get the masked command
     *
     * @param id the mask id
     * @return the masked command
     */
    public static String getCommand(final UUID id) {
        String command = commands.getOrDefault(id, null);
        if (command != null) {
            return command;
        }

        return "";
    }

    /**
     * Consume a command
     *
     * @param id the command ID
     */
    public static void consume(final UUID id) {
        commands.remove(id);
    }

    /**
     * Get the arguments
     *
     * @param id the mask ID
     * @return the arguments
     */
    public static String[] getArguments(final UUID id) {
        String[] data = args.getOrDefault(id, null);
        if (data != null) {
            return args.remove(id);
        }
        commands.remove(id);

        return new String[0];
    }

    /**
     * Hide a string
     *
     * @param string the string to hide
     * @return the hidden string
     */
    private static String hide(final String string) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); i++) builder.append('*');

        return builder.toString();
    }
}
