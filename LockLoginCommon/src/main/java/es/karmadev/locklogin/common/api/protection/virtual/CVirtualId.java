package es.karmadev.locklogin.common.api.protection.virtual;

import es.karmadev.api.file.FileEncryptor;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.SecretStore;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;

import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

public class CVirtualId implements VirtualID {

    private final String virtualID;
    static {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.getRuntime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
    }

    @SuppressWarnings("deprecation")
    public CVirtualId() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.getRuntime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

        Configuration configuration = plugin.configuration();
        SecretStore store = configuration.secretKey();

        Path legacy_id = plugin.workingDirectory().resolve("cache").resolve("virtual_id.kf");
        Path virtual_id = plugin.workingDirectory().resolve("data").resolve("virtual.id");

        if (Files.exists(legacy_id)) {
            KarmaMain legacy = new KarmaMain(legacy_id);
            if (legacy.isSet("virtual_key")) {
                Element<?> virtualKey = legacy.get("virtual_key");
                if (virtualKey.isPrimitive() && virtualKey.getAsPrimitive().isString()) {
                    PathUtilities.write(virtual_id, Base64.getDecoder().decode(virtualKey.getAsString()));
                    PathUtilities.destroy(legacy_id);
                }
            }
        }

        FileEncryptor encryptor = new FileEncryptor(virtual_id, store.token());
        if (Files.exists(virtual_id)) {
            try {
                encryptor.tryDecrypt(new IvParameterSpec(store.iv()));
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        String tmpId = PathUtilities.read(virtual_id);
        if (ObjectUtils.isNullOrEmpty(tmpId) || tmpId.equalsIgnoreCase("null")) {
            tmpId = StringUtils.generateString();
            PathUtilities.write(virtual_id, tmpId);
        }
        virtualID = tmpId;

        if (Files.exists(virtual_id)) {
            try {
                encryptor.tryEncrypt(new IvParameterSpec(store.iv()));
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Virtualize the input
     *
     * @param input the input to virtualize
     * @return the virtualized input
     */
    @Override
    public VirtualizedInput virtualize(final String input) {
        String tmp = virtualID + input;
        char[] normal = tmp.toCharArray();

        char[] shuffled = new char[normal.length];
        int[] map = new int[normal.length + 1];
        map[0] = virtualID.length();

        List<Integer> generated = new ArrayList<>();
        for (int i = 0; i < normal.length; i++) {
            int newPosition = (int) Math.floor(Math.random() * normal.length);
            if (generated.contains(newPosition)) {
                while (generated.contains(newPosition)) newPosition = (int) Math.floor(Math.random() * normal.length);
            }
            generated.add(newPosition);

            map[newPosition + 1] = i;
            shuffled[i] = normal[newPosition];
        }

        String product = Stream.of(shuffled).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
        return CVirtualInput.of(map, true, product.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Virtualize the input
     *
     * @param input      the input to virtualize
     * @param references the references to force
     * @return the vritualized input
     *
     * @throws IllegalStateException if the input doesn't match the references length
     */
    @Override
    public VirtualizedInput virtualize(final String input, final int[] references) throws IllegalStateException {
        String tmp = virtualID + input;

        char[] provided = tmp.toCharArray();
        char[] converted = new char[provided.length];

        try {
            for (int i = 0; i < provided.length; i++) {
                int r = references[i + 1];
                converted[r] = provided[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalStateException("Cannot virtualize for invalid length input");
        }

        String raw = Stream.of(converted)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
        return CVirtualInput.of(references, true, raw.getBytes(StandardCharsets.UTF_8));
    }
}
