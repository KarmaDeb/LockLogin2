package es.karmadev.locklogin.common.api.protection.legacy.virtual;

import es.karmaconfigs.api.common.karma.file.KarmaMain;
import es.karmaconfigs.api.common.karma.file.element.types.Element;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualId;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualInput;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents the legacy virtual ID system
 */
public class LegacyID implements VirtualID {

    private final String virtualID;
    static {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.getRuntime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, CVirtualId.class, "static {}");
    }

    @SuppressWarnings("deprecation")
    public LegacyID() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.getRuntime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, CVirtualId.class, "CVirtualId()");

        Path legacy_id = plugin.workingDirectory().resolve("cache").resolve("virtual_id.kf");
        String tmpKey = "";
        if (Files.exists(legacy_id)) {
            KarmaMain legacy = new KarmaMain(legacy_id);
            if (legacy.isSet("virtual_key")) {
                Element<?> virtualKey = legacy.get("virtual_key");
                if (virtualKey.isPrimitive() && virtualKey.getAsPrimitive().isString()) {
                    tmpKey = virtualKey.getAsString();
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
