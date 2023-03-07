package es.karmadev.locklogin.api.plugin.file;

import es.karmadev.locklogin.api.plugin.file.section.*;

import java.io.Serializable;

/**
 * LockLogin configuration
 */
public interface Configuration {

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
     * Get if the bedrock players should be
     * able to bypass login steps
     *
     * @return if bedrock clients are able to
     * bypass login
     */
    boolean bedrockLogin();

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
