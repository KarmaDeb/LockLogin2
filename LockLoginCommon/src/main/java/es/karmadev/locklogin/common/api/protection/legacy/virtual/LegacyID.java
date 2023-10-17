package es.karmadev.locklogin.common.api.protection.legacy.virtual;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualInput;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the legacy virtual ID system
 */
public class LegacyID implements VirtualID {

    private final String virtualID;

    public LegacyID() {
        LockLogin plugin = CurrentPlugin.getPlugin();

        Path legacy_id = plugin.workingDirectory().resolve("cache").resolve("virtual_id.kf");
        String tmpKey = "";
        if (Files.exists(legacy_id)) {
            Pattern virtualKePattern = Pattern.compile("'virtual_key' -> [\"'].*[\"']");

            List<String> lines = PathUtilities.readAllLines(legacy_id);
            for (String line : lines) {
                int end = line.length() - 1;

                Matcher virtualMatcher = virtualKePattern.matcher(line);
                if (virtualMatcher.find()) {
                    int being = virtualMatcher.start() + "'virtual_key' -> '".length();
                    tmpKey = line.substring(being, end);

                    break;
                }
            }
        }

        virtualID = tmpKey;
    }

    /**
     * Virtualize the input
     *
     * @param input the input to virtualize
     * @return the virtualized input
     */
    @Override
    public VirtualizedInput virtualize(final String input) {
        if (virtualID.isEmpty()) {
            return CVirtualInput.raw(input.getBytes(StandardCharsets.UTF_8));
        }

        int[] references = new int[virtualID.length()];
        for (int i  = 0; i < virtualID.length(); i++) {
            references[i] = i;
        }

        return CVirtualInput.of(references, true, (virtualID + input).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Virtualize the input
     *
     * @param input      the input to virtualize
     * @param reference the references to force
     * @return the virtualized input
     * @throws IllegalStateException if the input doesn't match the references length
     */
    @Override
    public VirtualizedInput virtualize(final String input, final int[] reference) throws IllegalStateException {
        if (reference.length != virtualID.length()) {
            throw new IllegalStateException("Cannot virtualize for invalid length input");
        }

        for (int i : reference) {
            if (reference[i] != i) {
                throw new IllegalStateException("Cannot virtualize for non-legacy virtualized references");
            }
        }

        if (virtualID.isEmpty()) {
            return CVirtualInput.raw(input.getBytes(StandardCharsets.UTF_8));
        }

        return CVirtualInput.of(reference.clone(), true, (virtualID + input).getBytes(StandardCharsets.UTF_8));
    }
}
