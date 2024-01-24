package es.karmadev.locklogin.common.api.plugin.service.mail;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.plugin.service.mail.MailMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Builder @Getter @Accessors(fluent = true)
public class CMailMessage implements MailMessage {

    private final String origin;
    private final String subject;
    private final String message;

    @FieldNameConstants.Exclude
    private final List<File> attachments = new ArrayList<>();

    public void addAttachment(final File attachment) {
        if (attachments.contains(attachment)) return;
        attachments.add(attachment);
    }

    public void removeAttachment(final File attachment) {
        attachments.remove(attachment);
    }

    @Override
    public File[] attachments() {
        return attachments.toArray(new File[0]);
    }

    public static CMailMessageBuilder builder() {
        return new CMailMessageBuilder();
    }

    public static CMailTemplatedBuilder builder(final File templateFile) {
        return builder().forTemplate(templateFile);
    }

    public static class CMailMessageBuilder {

        /**
         * Set the message from a template
         * file
         *
         * @param templateFile the template file
         * @return the mail message
         */
        public CMailTemplatedBuilder forTemplate(final File templateFile) {
            String name = templateFile.getName();
            if (!name.endsWith(".html"))
                throw new IllegalArgumentException("Cannot set mail message from template file which is not a html file");

            this.message = PathUtilities.read(templateFile.toPath());
            return new CMailTemplatedBuilder(this);
        }
    }

    public static class CMailTemplatedBuilder {

        private String message;
        private String subject;
        private String origin;

        private CMailTemplatedBuilder(final CMailMessageBuilder builder) {
            this.message = builder.message;
            this.subject = builder.subject;
        }

        public CMailTemplatedBuilder subject(final String subject) {
            this.subject = subject;
            return this;
        }

        public CMailTemplatedBuilder origin(final String origin) {
            this.origin = origin;
            return this;
        }

        public CMailTemplatedBuilder applyPlaceholder(final String name, final Object replacement) {
            this.message = message.replace("%" + name + "%", String.valueOf(replacement));
            return this;
        }

        public CMailMessage build() {
            return new CMailMessage(origin, subject, message);
        }
    }
}
