package es.karmadev.locklogin.test.config;

import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.ProxyConfiguration;
import es.karmadev.locklogin.api.plugin.file.section.*;
import es.karmadev.locklogin.test.config.section.EncSection;

public class TemporalConfiguration implements Configuration {

    /**
     * Get the plugin proxy configuration
     *
     * @return the plugin proxy configuration
     */
    @Override
    public ProxyConfiguration proxy() {
        return null;
    }

    /**
     * Get the server name at the
     * eyes of the plugin
     *
     * @return the server name
     */
    @Override
    public String server() {
        return null;
    }

    /**
     * Get if the bedrock players should be
     * able to bypass login steps
     *
     * @return if bedrock clients are able to
     * bypass login
     */
    @Override
    public boolean bedrockLogin() {
        return false;
    }

    /**
     * Get the plugin secret key
     *
     * @return the plugin secret key
     */
    @Override
    public String secretKey() {
        return "WiYuVyZz4oZDcFlK61pbMXQ+X4cpn2SXA2qcumoQJfk=";
    }

    /**
     * Get the plugin statistics configuration
     *
     * @return the plugin statistics configuration
     */
    @Override
    public StatisticsConfiguration statistics() {
        return null;
    }

    /**
     * Get the plugin backup configuration
     *
     * @return the plugin backup configuration
     */
    @Override
    public BackupConfiguration backup() {
        return null;
    }

    /**
     * Get if the plugin overwrites the
     * server MOTD
     *
     * @return if the plugin overwrites the MOTD
     */
    @Override
    public boolean overwriteMotd() {
        return false;
    }

    /**
     * Get the plugin registration configuration
     *
     * @return the plugin register configuration
     */
    @Override
    public RegisterConfiguration register() {
        return null;
    }

    /**
     * Get the plugin login configuration
     *
     * @return the plugin login configuration
     */
    @Override
    public LoginConfiguration login() {
        return null;
    }

    /**
     * Get the plugin sessions configuration
     *
     * @return the plugin session configuration
     */
    @Override
    public SessionConfiguration session() {
        return null;
    }

    /**
     * Get if the plugin verifies IP addresses
     *
     * @return if the plugin validates IP
     * addresses
     */
    @Override
    public boolean verifyIpAddress() {
        return false;
    }

    /**
     * Get if the plugin verifies UUIDs
     *
     * @return if the plugin validates
     * UUIDs
     */
    @Override
    public boolean verifyUniqueIDs() {
        return false;
    }

    /**
     * Get if the plugin should hide
     * non logged clients from logged
     * clients
     *
     * @return if the plugin should
     * hide unlogged clients
     */
    @Override
    public boolean hideNonLogged() {
        return false;
    }

    /**
     * Get the plugin captcha configuration
     *
     * @return the plugin captcha configuration
     */
    @Override
    public CaptchaConfiguration captcha() {
        return null;
    }

    /**
     * Get the plugin encryption configuration
     *
     * @return the plugin encryption configuration
     */
    @Override
    public EncryptionConfiguration encryption() {
        return new EncSection();
    }

    /**
     * Get the plugin permission configuration
     *
     * @return the plugin permission configuration
     */
    @Override
    public PermissionConfiguration permission() {
        return null;
    }

    /**
     * Get the plugin password configuration
     *
     * @return the plugin password configuration
     */
    @Override
    public PasswordConfiguration password() {
        return null;
    }

    /**
     * Serialize the configuration
     *
     * @return the serialized configuration
     */
    @Override
    public String serialize() {
        return null;
    }

    /**
     * Load the configuration
     *
     * @param serialized the serialized configuration
     */
    @Override
    public void load(String serialized) {

    }
}
