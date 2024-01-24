package es.karmadev.locklogin.api.plugin.service.mail;

import java.io.File;

/**
 * LockLogin mail message
 */
public interface MailMessage {

    /**
     * Get the mail subject
     *
     * @return the subject
     */
    String subject();

    /**
     * Get the mail origin
     *
     * @return the origin
     */
    String origin();

    /**
     * Get the mail message
     *
     * @return the mail message
     */
    String message();

    /**
     * Get the mail attachments
     *
     * @return the attachments
     */
    File[] attachments();
}
