package es.karmadev.locklogin.common.user.storage.account.transiction;

import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.account.migration.Transictionable;
import lombok.Getter;
import lombok.experimental.Accessors;

public class CTransictionable implements Transictionable {

    private CTransictionable() {}

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

    public static CTransictionable from(final UserAccount account) {
        CTransictionable instance = new CTransictionable();
        instance.password = account.password();
        instance.pin = account.pin();
        instance._2fa = account._2FA();
        instance.panic = account.panic();
        instance.hasPassword = account.isRegistered();
        instance.hasPin = account.hasPin();
        instance.has2fa = account.has2FA();
        instance.isTokenSet = account._2faSet();
        instance.hasPanic = account.isProtected();

        return instance;
    }
}
