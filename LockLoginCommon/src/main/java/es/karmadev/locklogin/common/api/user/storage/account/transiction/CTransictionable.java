package es.karmadev.locklogin.common.api.user.storage.account.transiction;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.account.migration.Transictionable;
import es.karmadev.locklogin.api.user.session.UserSession;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.UUID;

public class CTransictionable implements Transictionable {

    private CTransictionable() {}

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
    private String _2fa;
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
    private boolean has2fa;
    @Getter
    @Accessors(fluent = true)
    private boolean isTokenSet;
    @Getter
    @Accessors(fluent = true)
    private boolean hasPanic;

    @Getter
    @Accessors(fluent = true)
    private boolean sessionPersistent;

    public static CTransictionable from(final LocalNetworkClient client) {
        CTransictionable instance = new CTransictionable();
        UserAccount account = client.account();
        UserSession session = client.session();

        instance.player = client.name();
        instance.uniqueId = client.uniqueId();

        if (account != null) {
            instance.password = account.password();
            instance.pin = account.pin();
            instance._2fa = account._2FA();
            instance.panic = account.panic();
            instance.hasPassword = account.isRegistered();
            instance.hasPin = account.hasPin();
            instance.has2fa = account.has2FA();
            instance.isTokenSet = account._2faSet();
            instance.hasPanic = account.isProtected();
        }
        instance.sessionPersistent = session != null && session.isPersistent();

        return instance;
    }
}
