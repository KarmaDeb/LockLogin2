package es.karmadev.locklogin.api.security.hash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
        JsonObject main = new JsonObject();
        JsonObject result = new JsonObject();
        VirtualizedInput vInput = product();

        result.addProperty("value", Base64.getEncoder().encodeToString(vInput.product()));
        result.addProperty("virtualized", vInput.valid());

        JsonObject references = new JsonObject();
        JsonArray array = new JsonArray();
        int[] ref = vInput.references();

        references.addProperty("size", ref.length);
        for (int i = 0; i < ref.length; i++) {
            JsonObject refObject = new JsonObject();
            refObject.addProperty("source", i);
            refObject.addProperty("target", ref[i]);

            array.add(refObject);
        }

        references.add("values", array);
        result.add("references", references);

        main.addProperty("method", hasher().name());
        main.add("product", result);

        Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
        String raw = gson.toJson(main);

        return Base64.getEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
