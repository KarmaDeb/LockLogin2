package es.karmadev.locklogin.api.plugin.file.mail;

import es.karmadev.api.object.ObjectUtils;

import javax.net.ssl.SNIHostName;
import java.util.List;

/**
 * Represents the configuration for
 * the integrated mailer
 */
public interface MailConfiguration {

    /**
     * Reload the mailer configuration
     *
     * @return if the configuration was able
     * to be reloaded
     */
    boolean reload();

    /**
     * Get the mailer host
     *
     * @return the mailer host
     */
    String getHost();

    /**
     * Get if the mail configuration makes
     * the mail service available
     *
     * @return if the service is available
     */
    default boolean isEnabled() {
        return !ObjectUtils.isNullOrEmpty(getHost());
    }

    /**
     * Get the mailer port
     *
     * @return the mailer port
     */
    int getPort();

    /**
     * Get if the connection is
     * performed under a secure channel
     *
     * @return if the connection is secure
     */
    boolean isSecure();

    /**
     * Get all the trusted hosts for the
     * smtp server
     *
     * @return the server trusted hosts
     * @deprecated not actually deprecated, but not implemented yet
     */
    @Deprecated
    List<SNIHostName> getTrustedHosts();

    /**
     * Get the mail username
     *
     * @return the mail username
     */
    String getUser();

    /**
     * Get the mail password
     *
     * @return the mail password
     */
    String getPassword();

    /**
     * Get the name to use when
     * sending emails
     *
     * @return the name to use
     */
    String getSendAs();
}
