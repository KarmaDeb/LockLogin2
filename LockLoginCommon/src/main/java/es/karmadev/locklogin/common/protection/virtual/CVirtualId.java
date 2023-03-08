package es.karmadev.locklogin.common.protection.virtual;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import ml.karmaconfigs.api.common.security.file.FileEncryptor;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

public class CVirtualId implements VirtualID {

    private final String virtualID;

    public CVirtualId() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Configuration configuration = plugin.configuration();

        Path virtual_id = plugin.workingDirectory().resolve("data").resolve("virtual.kf");
        KarmaMain main = new KarmaMain(virtual_id);

        main.create();

        Element<?> element = main.get("id");
        String tmpId = null;
        if (element.isPrimitive()) {
            ElementPrimitive primitive = element.getAsPrimitive();
            tmpId = primitive.asString();
        }

        if (StringUtils.isNullOrEmpty(tmpId) || tmpId.equalsIgnoreCase("null")) {
            tmpId = TokenGenerator.generateLiteral();
            main.set("id", new KarmaPrimitive(tmpId));
            main.save();
        }

        virtualID = tmpId;
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
     * @param refferences the refferences to force
     * @return the vritualized input
     *
     * @throws IllegalStateException if the input doesn't match the refferences length
     */
    @Override
    public VirtualizedInput virtualize(final String input, final int[] refferences) throws IllegalStateException {
        String tmp = virtualID + input;
        int startIndex = refferences[0];

        char[] provided = tmp.toCharArray();
        char[] converted = new char[provided.length];

        try {
            for (int i = 0; i < provided.length; i++) {
                int r = refferences[i + 1];
                converted[r] = provided[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalStateException("Cannot virtualize for invalid length input");
        }

        String raw = Stream.of(converted)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
        return CVirtualInput.of(refferences, true, raw.getBytes(StandardCharsets.UTF_8));
    }
}
