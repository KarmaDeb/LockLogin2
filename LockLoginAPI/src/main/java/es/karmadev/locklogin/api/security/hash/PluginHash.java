package es.karmadev.locklogin.api.security.hash;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * LockLogin hashing method
 */
public abstract class PluginHash {

    protected final Set<String> protected_properties = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected final Map<String, String> temporal_properties = new ConcurrentHashMap<>();
    protected final Map<String, String> permanent_properties = new ConcurrentHashMap<>();

    @Getter
    protected final boolean legacy;

    /**
     * Initialize the plugin hash
     *
     * @param properties the hash properties
     */
    public PluginHash(final String... properties) {
        this(false, properties);
    }

    /**
     * Initialize the plugin hash
     * @param legacy the hash legacy support status
     * @param properties the hash properties
     */
    public PluginHash(final boolean legacy, final String... properties) {
        this.legacy = legacy;
        protected_properties.addAll(Arrays.asList(properties));
    }

    protected final Set<String> protected_properties = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected final Map<String, String> temporal_properties = new ConcurrentHashMap<>();
    protected final Map<String, String> permanent_properties = new ConcurrentHashMap<>();

    /**
     * Initialize the plugin hash
     */
    public PluginHash(final String... properties) {
        protected_properties.addAll(Arrays.asList(properties));
    }

    /**
     * Get the hashing name
     *
     * @return the hashing name
     */
    public abstract String name();

    /**
     * Get the plugin legacy hasher
     *
     * @return the legacy hasher
     */
    @Nullable
    public LegacyPluginHash legacyHasher() {
        if (this instanceof LegacyPluginHash) {
            return (LegacyPluginHash) this; //Hack for not having to implement in our implementations
        }

        return null;
    }

    /**
     * Hash the input
     *
     * @param input the input to hash
     * @return the hashed input
     */
    public abstract HashResult hash(final String input);

    /**
     * Get if the provided hash result needs a rehash
     *
     * @param result the hash result
     * @return if the hash needs a rehash
     */
    public abstract boolean needsRehash(final HashResult result);

    /**
     * Verify a hash
     *
     * @param input the input to verify with
     * @param result the hashed input
     * @return if the input is correct
     */
    public abstract boolean verify(final String input, final HashResult result);

    /**
     * Write a property for the hash
     *
     * @param key the property key
     * @param value the property value
     * @param persistent if the property is persistent or
     *                   will only be used during the next hash
     */
    public void writeProperty(final String key, final String value, final boolean persistent) {
        if (protected_properties.contains(key)) {
            if (value == null || !persistent) return;
        }

        if (persistent) {
            temporal_properties.remove(key);
            permanent_properties.put(key, value);
        } else {
            if (!permanent_properties.containsKey(key)) {
                temporal_properties.put(key, value);
            }
        }
    }

    /**
     * Get a property
     *
     * @param key the property key
     * @return the property value
     */
    protected final String getProperty(final String key) {
        return permanent_properties.getOrDefault(key, temporal_properties.computeIfPresent(key, (k, v) -> null));
    }

    /**
     * Get if a property is permanent
     *
     * @param key the key
     * @return if the property is permanent
     */
    protected boolean isPermanent(final String key) {
        return permanent_properties.getOrDefault(key, null) != null;
    }

    /**
     * Converses a base64 string into a raw string, or vice-versa
     *
     * @param input the input to encode
     * @return the result
     */
    protected final String base64(final String input) {
        if (isBase64(input)) {
            return new String(Base64.getDecoder().decode(input.getBytes(StandardCharsets.UTF_8)));
        } else {
            return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Get if the input is a base64 string
     *
     * @param input the input to check
     * @return if the input is a base64 string
     */
    protected final boolean isBase64(final String input) {
        String regex = "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";

        Pattern patron = Pattern.compile(regex);
        return patron.matcher(input).matches();
    }
}
