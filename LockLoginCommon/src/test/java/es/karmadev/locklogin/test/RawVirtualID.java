package es.karmadev.locklogin.test;

import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.api.protection.virtual.CVirtualInput;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RawVirtualID implements VirtualID {

    private final String virtualID = "HelloWorld";

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

    public String resolve(final VirtualizedInput input) {
        int[] references = input.references();
        int startIndex = references[0];

        String result = new String(input.product());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < result.length(); i++) {
            int realIndex = references[i + 1];
            builder.append(result.charAt(realIndex));
        }

        return builder.substring(startIndex);
    }
}
