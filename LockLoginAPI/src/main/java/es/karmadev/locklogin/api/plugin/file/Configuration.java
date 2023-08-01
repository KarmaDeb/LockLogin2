package es.karmadev.locklogin.api.plugin.file;

import es.karmadev.locklogin.api.plugin.file.section.*;

/**
 * LockLogin configuration
 */
public interface Configuration {

    /**
     * Reload the configuration
     *
     * @return if the configuration
     * was able to be reloaded
     */
    boolean reload();

    /**
     * Get the plugin proxy configuration
     *
     * @return the plugin proxy configuration
     */
    ProxyConfiguration proxy();

    /**
     * Get the server name at the
     * eyes of the plugin
     *
     * @return the server name
     */
    String server();

    /**
     * Get the server communications
     * configuration
     *
     * @return the server communication
     * configuration
     */
    CommunicationConfiguration communications();

    /**
     * Get if the bedrock players should be
     * able to bypass login steps
     *
     * @return if bedrock clients are able to
     * bypass login
     */
    boolean bedrockLogin();

    /**
     * Get the plugin secret key
     *
     * @return the plugin secret key
     */
    SecretStore secretKey();

    /**
     * Get the plugin statistics configuration
     *
     * @return the plugin statistics configuration
     */
    StatisticsConfiguration statistics();

    /**
     * Get the plugin backup configuration
     *
     * @return the plugin backup configuration
     */
    BackupConfiguration backup();

    /**
     * Get the plugin premium configuration
     *
     * @return the plugin configuration for
     * premium users
     */
    PremiumConfiguration premium();

    /**
     * Get if the plugin overwrites the
     * server MOTD
     *
     * @return if the plugin overwrites the MOTD
     */
    boolean overwriteMotd();

    /**
     * Get the plugin registration configuration
     *
     * @return the plugin register configuration
     */
    RegisterConfiguration register();

    /**
     * Get the plugin login configuration
     *
     * @return the plugin login configuration
     */
    LoginConfiguration login();

    /**
     * Get the plugin sessions configuration
     *
     * @return the plugin session configuration
     */
    SessionConfiguration session();

    /**
     * Get if the plugin verifies IP addresses
     *
     * @return if the plugin validates IP
     * addresses
     */
    boolean verifyIpAddress();

    /**
     * Get if the plugin verifies UUIDs
     *
     * @return if the plugin validates
     * UUIDs
     */
    boolean verifyUniqueIDs();

    /**
     * Get if the plugin should hide
     * non logged clients from logged
     * clients
     *
     * @return if the plugin should
     * hide unlogged clients
     */
    boolean hideNonLogged();

    /**
     * Get the plugin messages interval configuration
     *
     * @return the plugin message configuration
     */
    MessageIntervalSection messageInterval();

    /**
     * Get the plugin captcha configuration
     *
     * @return the plugin captcha configuration
     */
    CaptchaConfiguration captcha();

    /**
     * Get the plugin encryption configuration
     *
     * @return the plugin encryption configuration
     */
    EncryptionConfiguration encryption();

    /**
     * Get the plugin movement configuration
     *
     * @return the plugin movement configuration
     */
    MovementConfiguration movement();

    /**
     * Get the plugin permission configuration
     *
     * @return the plugin permission configuration
     */
    PermissionConfiguration permission();

    /**
     * Get the plugin password configuration
     *
     * @return the plugin password configuration
     */
    PasswordConfiguration password();

    /**
     * Get the plugin brute force configuration
     *
     * @return the plugin bruteforce
     * settings
     */
    BruteForceConfiguration bruteForce();

    /**
     * Get if the plugin allows a client to
     * join to the server even though he's
     * already in, only if the address is
     * the same
     *
     * @return if the plugin filters connection
     * protection
     */
    boolean allowSameIp();

    /**
     * Get if the plugin enables base authentication
     *
     * @return if the plugin uses login and register
     */
    boolean enableAuthentication();

    /**
     * Get if the plugin enables the pin
     * login. Globally
     *
     * @return if the plugin uses pin
     */
    boolean enablePin();

    /**
     * Get if the plugin enables the
     * 2fa login. Globally
     *
     * @return if the plugin uses 2fa
     */
    boolean enable2fa();

    /**
     * Get the configuration for the
     * plugin updater
     *
     * @return the plugin updater configuration
     */
    UpdaterSection updater();

    /**
     * Get the configuration for the
     * plugin spawn
     *
     * @return the plugin spawn configuration
     */
    SpawnSection spawn();

    /**
     * Get if the player chat gets cleared when
     * he joins the server
     *
     * @return if the player chat gets cleared
     */
    boolean clearChat();

    /**
     * Get if the plugin validates the
     * usernames
     *
     * @return if the plugin verifies
     * names
     */
    boolean validateNames();

    /**
     * Get the plugin name check protocol
     *
     * @return the plugin name check protocl
     */
    int checkProtocol();

    /**
     * Get the plugin language
     *
     * @return the plugin language
     */
    String language();

    /**
     * Get the plugin database
     * configuration
     *
     * @return the database configuration
     */
    Database database();

    /**
     * Serialize the configuration
     *
     * @return the serialized configuration
     */
    String serialize();

    /**
     * Load the configuration
     *
     * @param serialized the serialized configuration
     */
    void load(final String serialized);

}
