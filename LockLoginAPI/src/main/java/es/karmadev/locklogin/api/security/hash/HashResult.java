package es.karmadev.locklogin.api.security.hash;

import es.karmadev.api.kson.JsonArray;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Hash result
 */
public abstract class HashResult {

    /**
     * Get the hasher
     *
     * @return the hasher
     */
    public abstract PluginHash hasher();

    /**
     * Get the final hash
     *
     * @return the hash
     */
    public abstract VirtualizedInput product();

    /**
     * Verify the input
     *
     * @param input the input
     * @return if the input is valid
     */
    public boolean verify(final String input) {
        PluginHash hasher = hasher();
        if (hasher != null) return hasher.verify(input, this);

        return false;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return serialize();
    }

    /**
     * Serialize the hash result
     *
     * @return the serialized string
     */
    public final String serialize() {
        JsonObject main = JsonObject.newObject("", "");
        JsonObject result = JsonObject.newObject("", "product");
        VirtualizedInput vInput = product();

        result.put("value", Base64.getEncoder().encodeToString(vInput.product()));
        result.put("virtualized", vInput.valid());

        JsonObject references = JsonObject.newObject("", "references");
        JsonArray array = JsonArray.newArray("", "values");
        int[] ref = vInput.references();

        references.put("size", ref.length);
        for (int i = 0; i < ref.length; i++) {
            JsonObject refObject = JsonObject.newObject("", "");
            refObject.put("source", i);
            refObject.put("target", ref[i]);

            array.add(refObject);
        }

        references.put("values", array);
        result.put("references", references);

        main.put("method", hasher().name());
        main.put("product", result);

        String raw = main.toString(false);
        return Base64.getEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
