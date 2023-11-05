package es.karmadev.locklogin.api.plugin.file.language;

import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.Alias;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.security.check.CheckResult;

import java.net.InetAddress;
import java.util.List;

/**
 * LockLogin messages
 */
@SuppressWarnings("unused")
public interface Messages {
    
    /**
     * Reload the messages file
     *
     * @return if the messages were able to be reloaded
     */
    boolean reload();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String prefix();

    /**
     * Get a plugin message
     *
     * @param entity message replace
     * @return plugin message
     */
    String join(final NetworkClient entity);

    /**
     * Get a plugin message
     *
     * @param entity message replace
     * @return plugin message
     */
    String leave(final NetworkClient entity);

    /**
     * Get a plugin message for modules
     *
     * @param permission message replace
     * @return plugin module message
     */
    String permissionError(final PermissionObject permission);

    /**
     * Get a plugin message
     *
     * @return the plugin message
     */
    String completeExtra();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String bungeeProxy();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String pinTitle();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String altTitle();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String infoTitle();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String nextButton();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String backButton();

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    String notVerified(final NetworkEntity target);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String alreadyPlaying();

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    String connectionError(final String name);

    /**
     * Get a plugin message
     *
     * @param name   message replace
     * @param amount message replace
     * @return plugin message
     */
    String altFound(final String name, final int amount);

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    String neverPlayer(final String name);

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    String targetAccessError(final String name);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String incorrectPassword();

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @return plugin message
     */
    String captcha(final String code);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String invalidCaptcha();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String sessionServerDisabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String sessionEnabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String sessionDisabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String sessionClosed();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String accountTerminated();

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @return plugin message
     */
    String register(final String code);

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param color message replace
     * @param time  message replace
     * @return plugin message
     */
    String registerBar(final String code, final String color, final long time);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String registered();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String alreadyRegistered();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String registerError();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String passwordInsecure();

    /**
     * Get a plugin messages
     *
     * @param client message replace
     * @return plugin message
     */
    String passwordWarning(final LocalNetworkClient client);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String registerTimeOut();

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param time message replace
     * @return plugin message
     */
    String registerTitle(final String code, final long time);

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param time message replace
     * @return plugin message
     */
    String registerSubtitle(final String code, final long time);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String maxRegisters();

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @return plugin message
     */
    String login(final String code);

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param color message replace
     * @param time  message replace
     * @return plugin message
     */
    String loginBar(final String code, final String color, final long time);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String logged();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String alreadyLogged();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String loginInsecure();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String loginTimeOut();

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param time message replace
     * @return plugin message
     */
    String loginTitle(final String code, final long time);

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param time message replace
     * @return plugin message
     */
    String loginSubtitle(final String code, final long time);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String loginForgot();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String loginForgotYes();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String loginForgotNo();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumEnabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumDisabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumError();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumAuth();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumServer();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumWarning();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumFailAuth();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumFailInternal();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumFailConnection();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumFailAddress();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumFailEncryption();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumFailPrecocious();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String premiumFailSession();

    /**
     * Get a plugin message
     *
     * @param result message argument
     * @return plugin message
     */
    String checkResult(final CheckResult result);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String pinUsages();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String pinSet();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String pinReseted();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String pinChanged();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String alreadyPin();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String noPin();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String setPin();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String resetPin();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String changePin();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String pinDisabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String pinLength();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String incorrectPin();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthUsages();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthSetupUsage();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthRemoveUsage();

    /**
     * Get a plugin message
     *
     * @return the plugin message
     */
    String gAuthDisableUsage();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthRequired();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthCorrect();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthAlready();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthIncorrect();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthSetupAlready();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthToggleError();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthDisabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthEnabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthNotEnabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthServerDisabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gauthLocked();

    /**
     * Get a plugin message
     *
     * @param codes message replace
     * @return plugin message
     */
    String gAuthScratchCodes(final List<String> codes);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthInstructions();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String gAuthLink();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String panicLogin();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String panicTitle();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String panicSubtitle();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String panicMode();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @SuppressWarnings("unused")
    String panicDisabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String panicAlready();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String panicRequested();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String panicEnabled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String tokenLink();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffSetup();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffSetupSuccess();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffInvalid();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffUsage();

    /**
     * Get a plugin message
     *
     * @param issuer message replace
     * @param action message replace
     * @param target message replace
     * @return plugin message
     */
    String staffEcho(final NetworkEntity issuer, final String action, final NetworkEntity target);

    /**
     * Get a plugin message
     *
     * @param issuer message replace
     * @param action message replace
     * @return plugin message
     */
    String staffClientEcho(final LocalNetworkClient issuer, final String action);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffToggleUsage();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffToggleSuccess();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffToggleFailure();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffRegisterUsage();

    /**
     * Get a plugin message
     *
     * @param client message replace
     * @param password message replace
     * @return plugin message
     */
    String staffRegisterSuccess(final LocalNetworkClient client, final String password);

    /**
     * Get a plugin message
     *
     * @param client message replace
     * @return plugin message
     */
    String staffRegisterFailure(final String client);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffLoginUsage();

    /**
     * Get a plugin message
     *
     * @param client message replace
     * @return plugin message
     */
    String staffLoginSuccess(final NetworkClient client);

    /**
     * Get a plugin message
     *
     * @param client message replace
     * @return plugin message
     */
    String staffLoginFailure(final String client);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffLogoutUsage();

    /**
     * Get a plugin message
     *
     * @param client message replace
     * @return plugin message
     */
    String staffLogoutSuccess(final NetworkClient client);

    /**
     * Get a plugin message
     *
     * @param client message replace
     * @return plugin message
     */
    String staffLogoutFailure(final String client);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffUnregisterUsage();

    /**
     * Get a plugin message
     *
     * @param client message replace
     * @return plugin message
     */
    String staffUnregisterSuccess(final LocalNetworkClient client);

    /**
     * Get a plugin message
     *
     * @param client message replace
     * @return plugin message
     */
    String staffUnregisterFailure(final String client);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffLookupUsage();

    /**
     * Get a plugin message
     *
     * @param filter message replace
     * @return plugin message
     */
    String staffLookupEmpty(final String filter);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String staffUnbanUsage();

    /**
     * Get a plugin message
     *
     * @param address message replace
     * @return plugin message
     */
    String staffUnbanSuccess(final InetAddress address);

    /**
     * Get a plugin message
     *
     * @param address message replace
     * @return plugin message
     */
    String staffUnbanFailure(final InetAddress address);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnUsage();

    /**
     * Get a plugin message
     *
     * @param rawLocation message replace
     * @return plugin message
     */
    String spawnSet(final String rawLocation);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnNotSet();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnUnset();

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    String spawnTeleportTime(final int time);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnTeleport();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnTeleportAdmin();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnTeleportNear();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnCancelled();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnBack();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String spawnNoBack();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String locationsReset();

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    String locationReset(final String name);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String locationsFixed();

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    String locationFixed(final String name);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String resetLocUsage();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String alias();

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    String aliasCreated(final Alias alias);

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    String aliasDestroyed(final Alias alias);

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    String aliasExists(final Alias alias);

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    String aliasNotFound(final String alias);

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    String addedPlayer(final Alias alias, final String... players);

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    String removedPlayer(final Alias alias, final String... players);

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    String playerNotIn(final Alias alias, final String... players);

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    String playerAlreadyIn(final Alias alias, final String... players);

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    String ipBlocked(final long time);

    /**
     * Get a plugin message
     *
     * @param chars message replace
     * @return plugin message
     */
    String illegalName(final String chars);

    /**
     * Get a plugin message
     *
     * @param name       messages replace
     * @param knownNames message replace
     * @return plugin message
     */
    String multipleNames(final String name, final String... knownNames);

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String uuidFetchError();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String ipProxyError();

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    String bedrockJava();

    /**
     * Serialize the messages
     *
     * @return the serialized messages
     */
    String serialize();

    /**
     * Load the messages
     *
     * @param serialized the serialized messages
     */
    void load(final String serialized);
}
