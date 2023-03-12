package es.karmadev.locklogin.common.api.dependency;

import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Checksum implements DependencyChecksum {

    private final Map<String, Long> values = new ConcurrentHashMap<>();

    public void define(final String name, final long value) {
        values.put(name, value);
    }

    /**
     * Get the checksum value
     *
     * @param name the check name
     * @return the checksum value
     */
    @Override
    public long value(final String name) {
        return values.getOrDefault(name, 0l);
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
        for (String key : values.keySet()) {
            long value = values.getOrDefault(key, 0l);
            if (other.value(key) != value) {
                return false;
            }
        }

        return true;
    }
}
