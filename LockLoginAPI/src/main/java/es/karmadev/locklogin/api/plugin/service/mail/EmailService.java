package es.karmadev.locklogin.api.plugin.service.mail;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.service.PluginService;

/**
 * LockLogin mailer service
 */
public interface EmailService extends PluginService {

    /**
     * Send a mail message to the recipient
     *
     * @param target the recipient
     * @param message the message to send
     */
    void send(final String target, final MailMessage message);

    /**
     * Send a mail message to the recipient
     *
     * @param client the client to send the email to
     * @param message the message to send
     */
    void send(final LocalNetworkClient client, final MailMessage message);
}
