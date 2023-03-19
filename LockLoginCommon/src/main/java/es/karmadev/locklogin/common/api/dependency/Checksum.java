package es.karmadev.locklogin.common.api.dependency;

import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Checksum implements DependencyChecksum {

    private final LockLoginDependency dependency;
    private final Map<String, Long> values = new ConcurrentHashMap<>();

    public Checksum(final LockLoginDependency owner) {
        dependency = owner;
    }

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
        return values.getOrDefault(name, 0L);
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

        return true;
    }
}
