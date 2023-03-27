package es.karmadev.locklogin.api.plugin.service.mail;

/**
 * LockLogin mailer service
 */
public interface EmailService {

    /**
     * Send a mail message to the recipient
     *
     * @param recipient the target
     * @param message the message to send
     */
    void send(final String recipient, final MailMessage message);

    /**
     * Receive a mail message
     *
     * @return the mail message
     */
    MailMessage receive();
}
