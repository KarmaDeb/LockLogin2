package es.karmadev.locklogin.common.plugin.service.backup;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.security.backup.store.BackupStorage;
import es.karmadev.locklogin.api.security.backup.store.UserBackup;
import lombok.Getter;
import lombok.experimental.Accessors;
import ml.karmaconfigs.api.common.data.path.PathUtilities;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class CBackupStorage implements BackupStorage {

    private Path file;

    @Getter
    @Accessors(fluent = true)
    private String id;

    @Getter
    @Accessors(fluent = true)
    private UserBackup[] accounts;

    private long creation;

    private CBackupStorage() {}

    /**
     * Find a backup for the client
     *
     * @param client the client
     * @return the client backup if any
     */
    @Override
    public UserBackup find(final LocalNetworkClient client) {
        return Arrays.stream(accounts).filter((backup) -> backup.account() == client.id()).findFirst().orElse(null);
    }

    /**
     * Get when the backup was created
     *
     * @return the backup creation date
     */
    @Override
    public Instant creation() {
        return Instant.ofEpochMilli(creation);
    }

    /**
     * Destroy the backup
     *
     * @return if the backup could be removed
     */
    @Override
    public boolean destroy() {
        return PathUtilities.destroyWithResults(file);
    }

    public static BackupStorage forAccounts(final String id, final List<UserBackup> backups, final Instant creation, final Path file) {
        CBackupStorage instance = new CBackupStorage();
        instance.file = file;
        instance.id = id;
        instance.accounts = backups.toArray(new UserBackup[0]);
        instance.creation = creation.toEpochMilli();

        return instance;
    }
}
