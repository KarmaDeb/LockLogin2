package es.karmadev.locklogin.api.security.hash;

import es.karmadev.api.shaded.google.gson.Gson;
import es.karmadev.api.shaded.google.gson.GsonBuilder;
import es.karmadev.api.shaded.google.gson.JsonArray;
import es.karmadev.api.shaded.google.gson.JsonObject;
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
     * @apiNote In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * The string output is not necessarily stable over time or across
     * JVM invocations.
     * @implSpec The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
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
