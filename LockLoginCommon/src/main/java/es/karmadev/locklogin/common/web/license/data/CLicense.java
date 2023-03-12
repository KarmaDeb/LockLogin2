package es.karmadev.locklogin.common.web.license.data;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.license.License;
import es.karmadev.locklogin.api.plugin.license.data.LicenseExpiration;
import es.karmadev.locklogin.api.plugin.license.data.LicenseOwner;
import lombok.Builder;
import ml.karmaconfigs.api.common.data.path.PathUtilities;

import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Builder
public class CLicense implements License {

    @Builder.Default
    private transient Path license_file = CurrentPlugin.getPlugin().workingDirectory().resolve("data").resolve("license.dat");

    private String base64;
    private String version;
    private String sync;
    private String com;
    private String name;
    private String contact;
    private long created;
    private long expiration;
    private int proxies;
    private long storage;
    private boolean free;

    /**
     * Get the license version
     *
     * @return the license version
     */
    @Override
    public String version() {
        return version;
    }

    /**
     * Get the license file location
     *
     * @return the license file location
     */
    @Override
    public Path location() {
        return license_file;
    }

    /**
     * Set the license location
     *
     * @param location the license location
     * @throws NotDirectoryException if the location is not a directory
     */
    @Override
    public void setLocation(final Path location) throws NotDirectoryException {
        if (!Files.exists(location)) PathUtilities.createDirectory(location);
        if (location == null || !Files.isDirectory(location)) throw new NotDirectoryException(PathUtilities.getPrettyPath(location));

        license_file = location.resolve("license.dat");
    }

    /**
     * Get the license base64 value
     *
     * @return the license base64 value
     */
    @Override
    public String base64() {
        return base64;
    }

    /**
     * Get the license synchronization key
     *
     * @return the license synchronization key
     */
    @Override
    public String synchronizationKey() {
        return sync;
    }

    /**
     * Get the license communication key
     *
     * @return the license communication key
     */
    @Override
    public String communicationKey() {
        return com;
    }

    /**
     * Get the license owner
     *
     * @return the license owner
     */
    @Override
    public LicenseOwner owner() {
        return CLicenseOwner.of(name, contact);
    }

    /**
     * Get when this license expires
     *
     * @return the license expiration time
     */
    @Override
    public LicenseExpiration expiration() {
        return new CLicenseDate(created, expiration);
    }

    /**
     * Get the maximum amount of proxies
     * allowed in this license
     *
     * @return the proxies amount
     */
    @Override
    public int maxProxies() {
        return proxies;
    }

    /**
     * Get the license backup storage
     * size
     *
     * @return the license backup storage
     */
    @Override
    public long backupStorage() {
        return storage;
    }

    /**
     * Get if the license is free
     *
     * @return if the license is free
     */
    @Override
    public boolean free() {
        return free;
    }

    /**
     * Get if the license is installed
     *
     * @return the license install status
     */
    @Override
    public boolean installed() {
        License installed = CurrentPlugin.getPlugin().license();
        if (installed == null) return license_file != null && Files.exists(license_file);

        return license_file != null && Files.exists(license_file) && installed.base64().equals(base64);
    }

    /**
     * Install the license
     *
     * @return if the license was able to be installed
     */
    @Override
    public boolean install() {
        if (Files.exists(license_file)) return false; //We won't and shouldn't overwrite licenses while the plugin is running

        LockLogin plugin = CurrentPlugin.getPlugin();
        License installed = plugin.license();
        if (license_file != null && !Files.exists(license_file)) {
            byte[] data = Base64.getDecoder().decode(base64);
            try {
                PathUtilities.create(license_file);
                Files.write(license_file, data);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        if (installed == null || !installed.base64().equals(base64)) {
            try {
                plugin.updateLicense(this);
                return true;
            } catch (SecurityException ignored) {}
        }

        return false;
    }

    /**
     * Force the installation of this license, this won't
     * make checks for if the license file exists. Use with
     * caution
     */
    @Override
    public void forceInstall() {
        byte[] data = Base64.getDecoder().decode(base64);
        try {
            PathUtilities.create(license_file);
            Files.write(license_file, data);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Merge the other license with this one
     *
     * @param other the other license
     * @return the amount of changes
     */
    @Override
    public String[] merge(final License other) {
        List<String> changes = new ArrayList<>();

        if (!base64.equals(other.base64())) {
            base64 = other.base64();
        }

        if (!version.equals(other.version())) {
            changes.add("version");
            version = other.version();
        }

        if (!sync.equalsIgnoreCase(other.synchronizationKey())) {
            changes.add("synchronization key");
            sync = other.synchronizationKey();
        }

        if (!com.equalsIgnoreCase(other.communicationKey())) {
            changes.add("communication key");
            sync = other.synchronizationKey();
        }

        if (!name.equalsIgnoreCase(other.owner().name())) {
            changes.add("name");
            name = other.owner().name();
        }

        if (!contact.equalsIgnoreCase(other.owner().contact())) {
            changes.add("contact");
            contact = other.owner().contact();
        }

        if (created != other.expiration().granted().toEpochMilli()) {
            changes.add("creation");
            created = other.expiration().granted().toEpochMilli();
        }

        if (other.expiration().hasExpiration() && expiration != other.expiration().expiration().toEpochMilli()) {
            changes.add("expiration");
            expiration = other.expiration().expiration().toEpochMilli();
        }

        if (proxies != other.maxProxies()) {
            changes.add("proxies");
            proxies = other.maxProxies();
        }

        if (storage != other.backupStorage()) {
            changes.add("storage");
            storage = other.backupStorage();
        }

        if (free != other.free()) {
            changes.add("free");
            free = other.free();
        }

        return changes.toArray(new String[0]);
    }
}
