package es.karmadev.locklogin.api.plugin.service.mail;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

/**
 * LockLogin mailer service
 */
public interface EmailService {

    /**
     * Send a mail message to the recipient
     *
     * @param client the client to send the email to
     * @param recipient the target
     * @param message the message to send
     */
    void send(final LocalNetworkClient client, final String recipient, final MailMessage message);
}
