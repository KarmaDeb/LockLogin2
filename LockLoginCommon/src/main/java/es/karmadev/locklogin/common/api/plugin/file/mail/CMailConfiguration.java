package es.karmadev.locklogin.common.api.plugin.file.mail;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.api.file.yaml.handler.YamlHandler;
import es.karmadev.api.file.yaml.handler.YamlReader;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.mail.MailConfiguration;

import javax.net.ssl.SNIHostName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CMailConfiguration implements MailConfiguration {

    private final YamlFileHandler yaml;

    public CMailConfiguration(final LockLogin plugin) {
        Path file = plugin.workingDirectory().resolve("mailer.yml");
        if (!Files.exists(file)) {
            PathUtilities.copy(plugin, "plugin/yaml/configuration/mailer/config.yml", file);
        }

        try {
            YamlReader reader = new YamlReader(plugin.loadResource("plugin/yaml/configuration/mailer/config.yml"));
            yaml = YamlHandler.load(file, reader);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        yaml.validate();
    }

    /**
     * Reload the mailer configuration
     *
     * @return if the configuration was able
     * to be reloaded
     */
    @Override
    public boolean reload() {
        return yaml.reload();
    }

    /**
     * Get the mailer host
     *
     * @return the mailer host
     */
    @Override
    public String getHost() {
        return yaml.getString("Host", "smtp.myserver.net");
    }

    /**
     * Get the mailer port
     *
     * @return the mailer port
     */
    @Override
    public int getPort() {
        return yaml.getInteger("Port", 25);
    }

    /**
     * Get if the connection is
     * performed under a secure channel
     *
     * @return if the connection is secure
     */
    @Override
    public boolean isSecure() {
        return yaml.getBoolean("TLS", true);
    }

    /**
     * Get all the trusted hosts for the
     * smtp server
     *
     * @return the server trusted hosts
     */
    @Override
    public List<SNIHostName> getTrustedHosts() {
        List<String> hosts = yaml.getList("TrustedHosts");
        List<SNIHostName> hostNames = new ArrayList<>();
        for (String host : hosts) {
            SNIHostName sni = new SNIHostName(host);
            hostNames.add(sni);
        }

        return hostNames;
    }

    /**
     * Get the mail username
     *
     * @return the mail username
     */
    @Override
    public String getUser() {
        return yaml.getString("User", "myuser");
    }

    /**
     * Get the mail password
     *
     * @return the mail password
     */
    @Override
    public String getPassword() {
        return yaml.getString("Password", "mypassword");
    }
}
