package es.karmadev.locklogin.common.api.plugin.file;

import es.karmadev.api.file.FileEncryptor;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.api.file.yaml.handler.YamlHandler;
import es.karmadev.api.file.yaml.handler.YamlReader;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.database.driver.Driver;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.ProxyConfiguration;
import es.karmadev.locklogin.api.plugin.file.database.Database;
import es.karmadev.locklogin.api.plugin.file.mail.MailConfiguration;
import es.karmadev.locklogin.api.plugin.file.section.*;
import es.karmadev.locklogin.api.plugin.file.spawn.SpawnConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.database.CDatabaseConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.mail.CMailConfiguration;
import es.karmadev.locklogin.common.api.plugin.file.section.*;
import es.karmadev.locklogin.common.api.plugin.file.spawn.CSpawnConfiguration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CPluginConfiguration implements Configuration {

    private final YamlFileHandler yaml;

    private final CProxyConfiguration proxy_config;
    private final CMailConfiguration mail_config;
    private final CDatabaseConfiguration database_config;
    private final CSpawnConfiguration spawn_config;

    /**
     * Initialize the plugin configuration
     */
    public CPluginConfiguration(final @NotNull LockLogin plugin) {
        Path file = plugin.workingDirectory().resolve("config.yml");
        if (!Files.exists(file)) {
            PathUtilities.copy(plugin, "plugin/yaml/configuration/config.yml", file);
        }

        try {
            YamlReader reader = new YamlReader(plugin.loadResource("plugin/yaml/configuration/config.yml"));
            yaml = YamlHandler.load(file, reader);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        proxy_config = new CProxyConfiguration();
        mail_config = new CMailConfiguration(plugin);

        Driver driver;
        String rawDriver = yaml.getString("DataDriver", "SQLITE").toLowerCase();
        switch (rawDriver) {
            case "mysql":
                driver = Driver.MySQL;
                break;
            case "mariadb":
                driver = Driver.MariaDB;
                break;
            case "sqlite":
            default:
                driver = Driver.SQLite;
                break;
        }

        yaml.validate();
        database_config = new CDatabaseConfiguration(plugin, driver);
        spawn_config = new CSpawnConfiguration(plugin);
    }

    /**
     * Reload the configuration
     *
     * @return if the configuration
     * was able to be reloaded
     */
    @Override
    public boolean reload() {
        return yaml.reload();
    }

    /**
     * Get the plugin proxy configuration
     *
     * @return the plugin proxy configuration
     */
    @Override
    public ProxyConfiguration proxy() {
        return proxy_config;
    }

    /**
     * Get the plugin mailer configuration
     *
     * @return the plugin mailer configuration
     */
    @Override
    public MailConfiguration mailer() {
        return mail_config;
    }

    /**
     * Get the server name at the
     * eyes of the plugin
     *
     * @return the server name
     */
    @Override
    public String server() {
        String name = yaml.getString("ServerName", "");
        if (ObjectUtils.isNullOrEmpty(name)) {
            name = "My server";
            yaml.set("ServerName", name);

            try {
                yaml.save();
                yaml.validate();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return name;
    }

    /**
     * Get the server communications
     * configuration
     *
     * @return the server communication
     * configuration
     */
    @Override
    public CommunicationConfiguration communications() {
        String host = yaml.getString("Communications.Server", "karmadev.es");
        int port = yaml.getInteger("Communications.Port", 2053);
        boolean ssl = yaml.getBoolean("Communications.SSL", true);

        return CComSection.of(host, port, ssl);
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
        return yaml.getBoolean("BedrockLogin", false);
    }

    /**
     * Get the plugin secret key
     *
     * @return the plugin secret key
     */
    @Override
    public SecretStore secretKey() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        String raw = yaml.getString("SecretKey", "");

        if (ObjectUtils.isNullOrEmpty(raw) || !raw.contains("$")) {
            String password = StringUtils.generateString(16);
            byte[] salt = new byte[16];
            SecureRandom secure = new SecureRandom();
            secure.nextBytes(salt);

            SecretKey secret = FileEncryptor.generateSecureKey(password, new String(salt));
            IvParameterSpec parameter = FileEncryptor.generateSecureSpec();
            if (secret == null) {
                plugin.err("Failed to generate plugin secret key. This will be harmfull in the future");

                return null;
            }

            SecretStore store = CSecretStore.of(secret.getEncoded(), parameter.getIV());
            String tokenBase = Base64.getEncoder().encodeToString(secret.getEncoded());
            String ivBase = Base64.getEncoder().encodeToString(parameter.getIV());

            yaml.set("SecretKey", tokenBase + "$" + ivBase);
            try {
                yaml.save();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return store;
        }
        String[] data = raw.split("\\$");
        String tokenBase = data[0];
        String ivBase = data[1];

        byte[] token = Base64.getDecoder().decode(tokenBase);
        byte[] iv = Base64.getDecoder().decode(ivBase);

        return CSecretStore.of(token, iv);
    }

    /**
     * Get the plugin statistics configuration
     *
     * @return the plugin statistics configuration
     */
    @Override
    public StatisticsConfiguration statistics() {
        boolean bstats = yaml.getBoolean("Statistics.bStats", true);
        boolean plugin = yaml.getBoolean("Statistics.plugin", true);

        return CStatsSection.of(bstats, plugin);
    }

    /**
     * Get the plugin backup configuration
     *
     * @return the plugin backup configuration
     */
    @Override
    public BackupConfiguration backup() {
        boolean enabled = yaml.getBoolean("Backup.Enable", true);
        int max = yaml.getInteger("Backup.Max", 5);
        int period = yaml.getInteger("Backup.Period", 30);
        int purge = yaml.getInteger("Backup.Purge", 7);

        return CBackupSection.of(enabled, max, period, purge);
    }

    /**
     * Get the plugin premium configuration
     *
     * @return the plugin configuration for
     * premium users
     */
    @Override
    public PremiumConfiguration premium() {
        boolean enabled = yaml.getBoolean("Premium.Enable", true);
        boolean auto = yaml.getBoolean("Premium.AutoToggle", true);
        boolean forceOffline = yaml.getBoolean("Premium.ForceUUID", true);

        return CPremiumSection.of(auto, enabled, forceOffline);
    }

    /**
     * Get if the plugin overwrites the
     * server MOTD
     *
     * @return if the plugin overwrites the MOTD
     */
    @Override
    public boolean overwriteMotd() {
        return yaml.getBoolean("BungeeMotd", true);
    }

    /**
     * Get the plugin registration configuration
     *
     * @return the plugin register configuration
     */
    @Override
    public RegisterConfiguration register() {
        boolean boss = yaml.getBoolean("Register.Boss", true);
        boolean blind = yaml.getBoolean("Register.Blind", false);
        int timeout = yaml.getInteger("Register.TimeOut", 60);
        int max = yaml.getInteger("Register.Max", 2);

        return CRegisterSection.of(boss, blind, timeout, max);
    }

    /**
     * Get the plugin login configuration
     *
     * @return the plugin login configuration
     */
    @Override
    public LoginConfiguration login() {
        boolean boss = yaml.getBoolean("Login.Boss", true);
        boolean blind = yaml.getBoolean("Login.Blind", false);
        int timeout = yaml.getInteger("Login.TimeOut", 60);

        return CLoginSection.of(boss, blind, timeout);
    }

    /**
     * Get the plugin sessions configuration
     *
     * @return the plugin session configuration
     */
    @Override
    public SessionConfiguration session() {
        boolean enable = yaml.getBoolean("Sessions.Enabled", true);
        int time = Math.max(0, Math.min(120, yaml.getInteger("Sessions.Time", 30)));

        return CSessionSection.of(enable, time);
    }

    /**
     * Get if the plugin verifies IP addresses
     *
     * @return if the plugin validates IP
     * addresses
     */
    @Override
    public boolean verifyIpAddress() {
        return yaml.getBoolean("IpHealthCheck", true);
    }

    /**
     * Get if the plugin verifies UUIDs
     *
     * @return if the plugin validates
     * UUIDs
     */
    @Override
    public boolean verifyUniqueIDs() {
        return yaml.getBoolean("UUIDValidator", true);
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
        return yaml.getBoolean("HideNonLogged", false);
    }

    /**
     * Get the plugin messages interval configuration
     *
     * @return the plugin message configuration
     */
    @Override
    public MessageIntervalSection messageInterval() {
        RegisterConfiguration register = register();
        LoginConfiguration login = login();

        int registration = Math.min(yaml.getInteger("MessagesInterval.Registration", 10), register.timeout());
        int logging = Math.min(yaml.getInteger("MessagesInterval.Logging", 10), login.timeout());

        return CMessageSection.of(registration, logging);
    }


    /**
     * Get the plugin captcha configuration
     *
     * @return the plugin captcha configuration
     */
    @Override
    public CaptchaConfiguration captcha() {
        boolean enabled = yaml.getBoolean("Captcha.Enabled", false);
        int length = Math.max(8, Math.min(16, yaml.getInteger("Captcha.Difficulty.Length", 8)));
        boolean letters = yaml.getBoolean("Captcha.Difficulty.Letters", true);
        boolean strikethrough = yaml.getBoolean("Captcha.Strikethrough.Enabled", true);
        boolean randomStrike = yaml.getBoolean("Captcha.Strikethrough.Random", true);

        return CCaptchaSection.of(enabled, length, letters, strikethrough, randomStrike);
    }

    /**
     * Get the plugin encryption configuration
     *
     * @return the plugin encryption configuration
     */
    @Override
    public EncryptionConfiguration encryption() {
        String algorithm = yaml.getString("Encryption.Algorithm", "argon2id");
        boolean encrypt = yaml.getBoolean("Encryption.Encrypt", true);
        boolean virtualID = yaml.getBoolean("Encryption.VirtualID", false);
        int memCost = yaml.getInteger("Encryption.HashCost.MemoryCost", 1024);
        int parallelism = yaml.getInteger("Encryption.HashCost.Parallelism", 22);
        int iterations = yaml.getInteger("Encryption.HashCost.Iterations", 2);

        return CEncryptionSection.of(algorithm, encrypt, virtualID, memCost, parallelism, iterations);
    }

    /**
     * Get the plugin movement configuration
     *
     * @return the plugin movement configuration
     */
    @Override
    public MovementConfiguration movement() {
        boolean allow = yaml.getBoolean("Movement.Allow", false);
        MovementConfiguration.MovementMethod method = MovementConfiguration.MovementMethod.TELEPORT;
        String rawMethod = yaml.getString("Movement.Method", "teleport").toLowerCase();
        if (rawMethod.equalsIgnoreCase("speed")) {
            method = MovementConfiguration.MovementMethod.SPEED;
        }
        int distance = yaml.getInteger("Movement.Distance", 10);

        return CMovementSection.of(allow, method, distance);
    }

    /**
     * Get the plugin permission configuration
     *
     * @return the plugin permission configuration
     */
    @Override
    public PermissionConfiguration permission() {
        boolean block_op = yaml.getBoolean("Permissions.BlockOperator", true);
        boolean remove_every = yaml.getBoolean("Permissions.RemoveEverything", true);
        boolean allow_wildcard = yaml.getBoolean("Permissions.AllowWildcard", false);
        List<String> unlog = yaml.getList("Permissions.UnLogged");
        List<String> log = yaml.getList("Permissions.Logged");

        return CPermissionSection.of(block_op, remove_every, allow_wildcard, unlog.toArray(new String[0]), log.toArray(new String[0]));
    }

    /**
     * Get the plugin password configuration
     *
     * @return the plugin password configuration
     */
    @Override
    public PasswordConfiguration password() {
        boolean success = yaml.getBoolean("Password.PrintSuccess", true);
        boolean block = yaml.getBoolean("Password.BlockUnsafe", true);
        boolean warn = yaml.getBoolean("Password.WarnUnsafe", true);
        boolean ignore = yaml.getBoolean("Password.IgnoreCommon", false);
        int length = yaml.getInteger("Password.Safety.MinLength", 10);
        int chars = yaml.getInteger("Password.Safety.Characters", 1);
        int numbers = yaml.getInteger("Password.Safety.Numbers", 2);
        int upper = yaml.getInteger("Password.Safety.Letters.Upper", 2);
        int lower = yaml.getInteger("Password.Safety.Letters.Lower", 5);

        int realMax = chars + numbers + upper + lower;
        if (realMax > length) {
            length = realMax;
        }

        return CPasswordSection.of(success, block, warn, ignore, length, chars, numbers, upper, lower);
    }

    /**
     * Get the plugin brute force configuration
     *
     * @return the plugin bruteforce
     * settings
     */
    @Override
    public BruteForceConfiguration bruteForce() {
        return null;
    }

    /**
     * Get if the plugin allows a client to
     * join to the server even though he's
     * already in, only if the address is
     * the same
     *
     * @return if the plugin filters connection
     * protection
     */
    @Override
    public boolean allowSameIp() {
        return yaml.getBoolean("AllowSameIp", true);
    }

    /**
     * Get if the plugin enables base authentication
     *
     * @return if the plugin uses login and register
     * @deprecated use specific methods instead
     */
    @Override @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0.5")
    public boolean enableAuthentication() {
        return yaml.getBoolean("Authentication.Register", true) && yaml.getBoolean("Authentication.Login", true);
    }

    /**
     * Get the plugin authentication settings
     *
     * @return the plugin authentication settings
     */
    @Override
    public AuthenticationConfiguration authSettings() {
        boolean register = yaml.getBoolean("Authentication.Register", true);
        boolean login = yaml.getBoolean("Authentication.Login", true);
        boolean pin = yaml.getBoolean("Authentication.Pin", true);
        boolean totp = yaml.getBoolean("Authentication.Totp", true);

        return CAuthSection.of(register, login, pin, totp);
    }

    /**
     * Get if the plugin enables the pin
     * login. Globally
     *
     * @return if the plugin uses pin
     */
    @Override @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0.5")
    public boolean enablePin() {
        return yaml.getBoolean("Authentication.Pin", true);
    }

    /**
     * Get if the plugin enables the
     * totp login. Globally
     *
     * @return if the plugin uses totp
     */
    @Override @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0.5")
    public boolean enable2fa() {
        return yaml.getBoolean("Authentication.Totp", true);
    }

    /**
     * Get the configuration for the
     * plugin updater
     *
     * @return the plugin updater configuration
     */
    @Override
    public UpdaterSection updater() {
        String channel = yaml.getString("Updater.Channel", "release").toLowerCase();
        boolean check = yaml.getBoolean("Updater.Check", true);
        int checkTime = yaml.getInteger("Updater.CheckTime", 10);

        String oChannel = channel;
        switch (channel) {
            case "release":
            case "beta":
            case "snapshot":
                break;
            case "rc":
            case "releasecandidate":
            case "release_candidate":
                channel = "beta";
                break;
            default:
                channel = "release";
        }

        if (!oChannel.equals(channel)) {
            yaml.set("Updater.Channel", channel);
            try {
                yaml.save();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return CUpdaterSection.of(BuildType.valueOf(channel.toUpperCase()), check, checkTime);
    }

    /**
     * Get the configuration for the
     * plugin spawn
     *
     * @return the plugin spawn configuration
     */
    @Override
    public SpawnConfiguration spawn() {
        return spawn_config;
    }

    /**
     * Get if the player chat gets cleared when
     * he joins the server
     *
     * @return if the player chat gets cleared
     */
    @Override
    public boolean clearChat() {
        return yaml.getBoolean("ClearChat", false);
    }

    /**
     * Get if the plugin validates the
     * usernames
     *
     * @return if the plugin verifies
     * names
     */
    @Override
    public boolean validateNames() {
        return yaml.getBoolean("CheckNames", true);
    }

    /**
     * Get the plugin name check regex
     *
     * @return the allowed name regex
     */
    @Override
    public Pattern namePattern() {
        String pattern = yaml.getString("NameCheckRegex", "^[A-Za-z-0-9_-]{3,16}$");
        try {
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException ex) {
            return Pattern.compile("^[A-Za-z-0-9_-]{3,16}$");
        }
    }

    /**
     * Get the plugin language
     *
     * @return the plugin language
     */
    @Override
    public String language() {
        String lang = yaml.getString("Language.Name", "English");
        switch (lang.toLowerCase()) {
            case "en_en":
                lang = "English";
                break;
            case "es_es":
                lang = "Spanish";
                break;
        }

        return lang.substring(0, 1).toUpperCase() + lang.substring(1).toLowerCase();
    }

    /**
     * Tries to define the plugin language
     *
     * @param newLanguage the new language
     */
    @Override
    public void setLanguage(final String newLanguage) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        if (plugin == null) return;

        yaml.set("Language.Name", newLanguage);
        try {
            yaml.save();
        } catch (IOException ex) {
            plugin.log(ex, "Failed to save configuration file");
        }
        yaml.validate();
    }

    /**
     * Get the plugin database
     * configuration
     *
     * @return the database configuration
     */
    @Override
    public Database database() {
        return database_config;
    }

    /**
     * Serialize the configuration
     *
     * @return the serialized configuration
     */
    @Override
    public String serialize() {
        return yaml.toString();
    }

    /**
     * Load the configuration
     *
     * @param serialized the serialized configuration
     */
    @Override
    public void load(final String serialized) {
        YamlFileHandler instance = YamlHandler.load(serialized);
        yaml.importFrom(instance, false);
    }
}