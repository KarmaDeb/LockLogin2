package es.karmadev.locklogin.common.plugin.secure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandWhitelist {

    private final static String[] DEFAULT = {
            "register",
            "reg",
            "login",
            "log",
            "2fa"
    };

    private final static List<String> custom = new ArrayList<>();

    public static boolean isBlacklisted(final String cmd) {
        String checkCMD = cmd;
        if (checkCMD.startsWith("/")) {
            checkCMD = checkCMD.substring(1);
        }

        return !custom.contains(checkCMD) && !Arrays.asList(DEFAULT).contains(checkCMD);
    }

    public static void allow(final String... commands) {
        custom.addAll(Arrays.asList(commands));
    }

    public static void disallow(final String... commands) {
        custom.removeAll(Arrays.asList(commands));
    }
}
