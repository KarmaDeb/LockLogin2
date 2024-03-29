package es.karmadev.locklogin.common.api.plugin.service.backup;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.security.backup.store.UserBackup;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

public class CBackupUser implements UserBackup {

    private CBackupUser() {}

    @Getter
    @Accessors(fluent = true)
    private int account;

    @Getter
    @Accessors(fluent = true)
    private String player;

    @Getter
    @Accessors(fluent = true)
    private UUID uniqueId;

    @Getter
    @Accessors(fluent = true)
    private HashResult password;
    @Getter
    @Accessors(fluent = true)
    private HashResult pin;
    @Getter
    @Accessors(fluent = true)
    private String totp;
    @Getter
    @Accessors(fluent = true)
    private HashResult panic;

    @Getter
    @Accessors(fluent = true)
    private boolean hasPassword;
    @Getter
    @Accessors(fluent = true)
    private boolean hasPin;
    @Getter
    @Accessors(fluent = true)
    private boolean hasTotp;
    @Getter
    @Accessors(fluent = true)
    private boolean isTotpSet;
    @Getter
    @Accessors(fluent = true)
    private boolean hasPanic;

    @Getter
    @Accessors(fluent = true)
    private boolean sessionPersistent;

    private long creation;

    /**
     * Get when the account was created
     *
     * @return the account creation date
     */
    @Override
    public Instant creation() {
        return Instant.ofEpochMilli(creation);
    }

    public static CBackupUser from(final UserAccount account, final LocalNetworkClient client) {
        CBackupUser instance = new CBackupUser();
        instance.account = client.id();
        instance.player = client.name();
        instance.uniqueId = client.uniqueId();
        instance.password = account.password();
        instance.pin = account.pin();
        instance.totp = account.totp();
        instance.panic = account.panic();
        instance.hasPassword = account.isRegistered();
        instance.hasPin = account.hasPin();
        instance.hasTotp = account.hasTotp();
        instance.isTotpSet = account.totpSet();
        instance.hasPanic = account.isProtected();
        instance.sessionPersistent = client.session() != null && client.session().isPersistent();
        instance.creation = client.creation().toEpochMilli();

        return instance;
    }
}
