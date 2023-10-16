package es.karmadev.locklogin.common.api.plugin.service.mail;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.MailConfiguration;
import es.karmadev.locklogin.api.plugin.service.mail.EmailService;
import es.karmadev.locklogin.api.plugin.service.mail.MailMessage;
import jakarta.activation.*;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CMailService implements EmailService {

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "LockLogin Mailer";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return false;
    }

    /**
     * Send a mail message to the recipient
     *
     * @param target  the recipient
     * @param message the message to send
     */
    @Override
    public void send(final String target, final MailMessage message) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) throw new IllegalStateException("Cannot send emails while plugin is not enabled");

        Configuration configuration = plugin.configuration();
        MailConfiguration mailConfig = configuration.mailer();

        if (ObjectUtils.isNullOrEmpty(mailConfig.getHost())) {
            plugin.logWarn("Tried to send email but email service has not been configured previously");
            return;
        }

        CompletableFuture.runAsync(() -> {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            Properties properties = new Properties();
            properties.put("mail.smtp.host", mailConfig.getHost());
            properties.put("mail.smtp.auth", String.valueOf(!ObjectUtils.isNullOrEmpty(mailConfig.getUser())));
            properties.put("mail.smtp.port", String.valueOf(mailConfig.getPort()));
            if (mailConfig.isSecure()) {
                properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                properties.put("mail.smtp.socketFactory.fallback", "false");
            }

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailConfig.getUser(), mailConfig.getPassword());
                }
            });

            try {
                Message mailMessage = new MimeMessage(session);
                mailMessage.setFrom(new InternetAddress("noreply@karmadev.es"));
                mailMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(target));
                mailMessage.setSubject(message.subject());

                Multipart multipart = new MimeMultipart();
                BodyPart body = new MimeBodyPart();
                body.setContent(message.message(), "text/html; charset=utf-8");
                multipart.addBodyPart(body);

                for (File file : message.attachments()) {
                    if (file.isDirectory()) {
                        recursive(file).forEach((fl) -> {
                            try {
                                BodyPart attachmentBody = new MimeBodyPart();
                                DataSource source = new FileDataSource(file);
                                attachmentBody.setDataHandler(new DataHandler(source));
                                attachmentBody.setFileName(file.getName());

                                multipart.addBodyPart(attachmentBody);
                            } catch (MessagingException ignored) {}
                        });
                        continue;
                    }

                    BodyPart attachmentBody = new MimeBodyPart();
                    DataSource source = new FileDataSource(file);
                    attachmentBody.setDataHandler(new DataHandler(source));
                    attachmentBody.setFileName(file.getName());

                    multipart.addBodyPart(attachmentBody);
                }

                mailMessage.setContent(multipart);
                mailMessage.saveChanges();

                Transport.send(mailMessage, mailConfig.getUser(), mailConfig.getPassword());
            } catch (MessagingException ex) {
                ex.printStackTrace();
                if (ex.getCause() instanceof SSLHandshakeException) {
                    plugin.log(ex, "Couldn't send email to {0} because a required SSL/TLS secure connection couldn't be established", target);
                    plugin.err("Couldn't send email to {0} because SSL/TLS was required but not available", target);
                    return;
                }

                plugin.log(ex, "Couldn't send email to {0}", target);
                plugin.err("An exception has raised while trying to send an email to {0}", target);
            }
        });

    }

    /**
     * Send a mail message to the recipient
     *
     * @param client    the client to send the email to
     * @param message   the message to send
     */
    @Override
    public void send(final LocalNetworkClient client, final MailMessage message) {
        send(client.account().email(), message);
    }

    private Collection<File> recursive(final File directory) {
        List<File> files = new ArrayList<>();
        File[] contents = directory.listFiles();
        if (contents == null) return files;

        for (File file : contents) {
            if (file.isDirectory()) {
                files.addAll(recursive(file));
                continue;
            }

            files.add(file);
        }

        return files;
    }
}
