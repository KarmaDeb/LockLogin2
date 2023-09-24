package es.karmadev.locklogin.common.api.dependency;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Checksum implements DependencyChecksum {

    private final LockLoginDependency dependency;
    private final Map<String, Long> values = new ConcurrentHashMap<>();
    private String hash;

    public Checksum(final LockLoginDependency owner) {
        dependency = owner;
    }

    protected void define(final String name, final long value) {
        values.put(name, value);
    }

    protected void hash(final String hash) {
        this.hash = hash;
    }

    /**
     * Get the checksum value
     *
     * @param name the check name
     * @return the checksum value
     */
    @Override
    public long value(final String name) {
        return values.getOrDefault(name, 0L);
    }

    /**
     * Get the checksum hash
     *
     * @return the check hash
     */
    @Override
    public String hash() {
        if (hash != null) return hash;

        Path file = dependency.file();
        byte[] data = PathUtilities.readBytes(file);

        try {
            MessageDigest md5 = MessageDigest.getInstance("md5");
            md5.update(data);
            byte[] result = md5.digest();

            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : result) {
                int rawInt = Byte.toUnsignedInt(b);
                hexBuilder.append(Integer.toString(rawInt, 16));
            }

            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException ex) {
            CurrentPlugin.getPlugin().log(ex, "Failed to verify integrity of {0}", dependency.name());
        }

        return null;
    }

    /**
     * Verifies if the provided checksum matches the
     * current one
     *
     * @param other the other checksum
     * @return if the checksums are the same
     */
    @Override
    public boolean matches(final DependencyChecksum other) {
        if (other == null) return false;

        for (String key : values.keySet()) {
            long value = values.getOrDefault(key, 0L);
            if (value == 0) return false;

            if (other.value(key) != value) {
                return false;
            }
        }

        return other.hash().equals(hash());
    }
}
