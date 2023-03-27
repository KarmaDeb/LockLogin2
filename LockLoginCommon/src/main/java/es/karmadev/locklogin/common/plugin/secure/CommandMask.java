package es.karmadev.locklogin.common.plugin.secure;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommandMask {

    private final static String[] filter = new String[]{
            "register",
            "reg",
            "login",
            "log",
            "account",
            "panic",
            "pin",
            "2fa"
    };

    private final static String[] subArguments = new String[]{
            "change",
            "unlock",
            "close",
            "remove",
            "delete",
            "alts",
            "session",
            "protect",
            "setup",
            "change"
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
            String[] cmdData = cmd.split(":");
            String pluginName = cmdData[0];

            cmdName = cmd.replaceFirst(pluginName + ":", "");
        }

        return Arrays.stream(filter).anyMatch(cmdName::startsWith);
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
     * Get the masked command
     *
     * @param id the mask id
     * @return the masked command
     */
    public static String getCommand(final UUID id) {
        String command = commands.getOrDefault(id, null);
        if (command != null) {
            return commands.remove(id);
        }

        return "";
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
