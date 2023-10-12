package es.karmadev.locklogin.common.api.user.storage.account.transiction;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.account.migration.Transitional;
import es.karmadev.locklogin.api.user.session.UserSession;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CTransitional implements Transitional {

    @Accessors(fluent = true)
    @Nullable
    private String player;

    @Accessors(fluent = true)
    @Nullable
    private UUID uniqueId;

    @Accessors(fluent = true)
    @Nullable
    private HashResult password;
    @Accessors(fluent = true)
    @Nullable
    private HashResult pin;
    @Accessors(fluent = true)
    @Nullable
    private String totp;
    @Accessors(fluent = true)
    @Nullable
    private HashResult panic;

    @Accessors(fluent = true)
    private boolean hasPassword;
    @Accessors(fluent = true)
    private boolean hasPin;
    @Accessors(fluent = true)
    private boolean hasTotp;
    @Accessors(fluent = true)
    private boolean isTotpSet;
    @Accessors(fluent = true)
    private boolean hasPanic;

    @Accessors(fluent = true)
    private boolean sessionPersistent;

    private static boolean warned = false;

    /**
     * Create a transitional instance from the
     * legacy data
     *
     * @param name the client name
     * @param uniqueId the client unique id
     * @param password the client password
     * @param pin the client pin
     * @param authToken the client auth token
     * @param panic the client panic token
     * @param authStatus the client auth token status
     * @return the transitional instance
     * @throws NullPointerException if some of the data mismatches
     * @throws IllegalStateException if the legacy hasher returns null
     */
    public static CTransitional fromLegacy(final @NotNull String name, final @NotNull UUID uniqueId,
                                         final @NotNull String password, final @NotNull String pin,
                                         final @NotNull String authToken, final @NotNull String panic,
                                         final boolean authStatus) throws NullPointerException, IllegalStateException {

        LockLogin plugin = CurrentPlugin.getPlugin();
        PluginHash legacyHasher = plugin.hasher().getMethod("legacy");
        if (legacyHasher == null) {
            throw new IllegalStateException("Tried to obtain legacy plugin hash but failed, have we been reloaded?");
        }
        if (!legacyHasher.isLegacy() && !warned) {
            warned = true;
            plugin.warn("Legacy plugin hasher not marked as legacy, this warning can be dismissed, but could cause problems in the future");
        }

        HashResult legacyPassword = null;
        HashResult legacyPin = null;
        HashResult legacyPanic = null;
        if (!ObjectUtils.isNullOrEmpty(password)) {
            legacyPassword = legacyHasher.hash(password);
            ObjectUtils.assertNullOrEmpty(legacyPassword, "Tried to hash legacy password using legacy hasher, but hasher returned null");
        }
        if (!ObjectUtils.isNullOrEmpty(pin)) {
            legacyPin = legacyHasher.hash(pin);
            ObjectUtils.assertNullOrEmpty(legacyPin, "Tried to hash legacy pin using legacy hasher, but hasher returned null");
        }
        if (!ObjectUtils.isNullOrEmpty(panic)) {
            legacyPanic = legacyHasher.hash(panic);
            ObjectUtils.assertNullOrEmpty(legacyPanic, "Tried to hash legacy panic token using legacy hasher, but hasher returned null");
        }

        return new CTransitional(name, uniqueId,
                legacyPassword, legacyPin, authToken,
                legacyPanic, legacyPassword != null,
                legacyPin != null,
                authStatus,
                !ObjectUtils.isNullOrEmpty(authToken),
                legacyPanic != null,
                false);
    }

    public static CTransitional from(final LocalNetworkClient client) {
        UserAccount account = client.account();
        UserSession session = client.session();

        String player = client.name();
        UUID uniqueId = client.uniqueId();

        HashResult password = null;
        HashResult pin = null;
        String authToken = null;
        HashResult panic = null;
        boolean hasPassword = false;
        boolean hasPin = false;
        boolean hasTotp = false;
        boolean isTotpSet = false;
        boolean hasPanic = false;
        boolean persistent = session != null && session.isPersistent();
        if (account != null) {
            password = account.password();
            pin = account.pin();
            authToken = account.totp();
            panic = account.panic();
            hasPassword = account.isRegistered();
            hasPin = account.hasPin();
            hasTotp = account.hasTotp();
            isTotpSet = account.totpSet();
            hasPanic = account.isProtected();
        }

        return new CTransitional(player, uniqueId,
                password, pin, authToken,
                panic, hasPassword, hasPin,
                hasTotp, isTotpSet, hasPanic,
                persistent);
    }
}
