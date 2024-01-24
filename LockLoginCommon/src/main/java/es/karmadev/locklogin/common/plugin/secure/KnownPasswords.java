package es.karmadev.locklogin.common.plugin.secure;

import java.util.Arrays;

/**
 * Known password list
 */
public class KnownPasswords {

    private static String[] known = new String[]{};

    /**
     * Mark the string as a known password
     *
     * @param input the input
     */
    public static void markAsKnown(final String input) {
        if (Arrays.stream(known).anyMatch(input::equalsIgnoreCase)) return;

        KnownPasswords.known = Arrays.copyOf(known, known.length + 1);
        known[known.length - 1] = input;
    }

    /**
     * Verify if the password is a known password
     *
     * @param input the password
     * @return if the password is known
     */
    public static boolean isKnown(final String input) {
        return Arrays.stream(known).anyMatch(input::equalsIgnoreCase);
    }
}
