package es.karmadev.locklogin.common.api.plugin.file;

import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.ProxyConfiguration;
import es.karmadev.locklogin.api.plugin.file.section.*;
import es.karmadev.locklogin.common.api.plugin.file.section.*;
import ml.karmaconfigs.api.common.karma.file.yaml.FileCopy;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.file.yaml.YamlReloader;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.security.file.FileEncryptor;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.string.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;

public class CPluginConfiguration implements Configuration {

    private final Path file = CurrentPlugin.getPlugin().workingDirectory().resolve("config.yml");
    private final KarmaYamlManager yaml;

    private final CProxyConfiguration proxy_config;

    /**
     * Initialize the plugin configuration
     */
    public CPluginConfiguration() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        FileCopy copy = new FileCopy(CPluginConfiguration.class, "config.yml");
        try {
            copy.copy(file);
        } catch (Throwable ex) {
            plugin.log(ex, "Failed to generate configuration file");
            plugin.err("Failed to generate configuration file. Default values will be used");
        }

        yaml = new KarmaYamlManager(file);
        proxy_config = new CProxyConfiguration();
    }

    /**
     * Reload the configuration
     *
     * @return if the configuration
     * was able to be reloaded
     */
    @Override
    public boolean reload() {
        YamlReloader reloader = yaml.getReloader();
        if (reloader != null) {
            reloader.reload();
            return true;
        }

        return false;
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
     * Get the server name at the
     * eyes of the plugin
     *
     * @return the server name
     */
    @Override
    public String server() {
        String name = yaml.getString("ServerName", "");
        if (StringUtils.isNullOrEmpty(name)) {
            name = "My server";
            yaml.set("ServerName", name);
            yaml.save(file.toFile(), (KarmaSource) CurrentPlugin.getPlugin(), "config.yml");
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
    public CommunicationSection communications() {
        String host = yaml.getString("Communications.Server", "karmadev.es");
        int port = yaml.getInt("Communications.Port", 2053);
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
        String raw = yaml.getString("SecretKey", "");

        if (StringUtils.isNullOrEmpty(raw)) {
            String password = TokenGenerator.generateLiteral(16);
            byte[] salt = new byte[16];
            SecureRandom secure = new SecureRandom();
            secure.nextBytes(salt);

            SecretKey secret = FileEncryptor.generateSecureKey(password, new String(salt));
            IvParameterSpec parameter = FileEncryptor.generateSecureSpec();
            if (secret == null) {
                LockLogin plugin = CurrentPlugin.getPlugin();
                plugin.err("Failed to generate plugin secret key. This will be harmfull in the future");

                return null;
            }

            SecretStore store = CSecretStore.of(secret.getEncoded(), parameter.getIV());
            raw = StringUtils.serialize(store);

            yaml.set("SecretKey", raw);
            yaml.save(file.toFile(), (KarmaSource) CurrentPlugin.getPlugin(), "config.yml");

            return store;
        }

        Object obj = StringUtils.load(raw);
        if (obj instanceof SecretStore) return (SecretStore) obj;

        return null;
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
        int max = yaml.getInt("Backup.Max", 5);
        int period = yaml.getInt("Backup.Period", 30);
        int purge = yaml.getInt("Backup.Purge", 7);

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
        boolean forceOffline = yaml.getBoolean("Premium.ForceUUID", true);

        return CPremiumSection.of(enabled, forceOffline);
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
        int timeout = yaml.getInt("Register.Timeout", 60);
        int max = yaml.getInt("Register.Max", 2);

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
        int timeout = yaml.getInt("Login.Timeout", 60);

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
        int time = Math.max(0, Math.min(120, yaml.getInt("Sessions.Time", 30)));

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

        int registration = Math.min(yaml.getInt("MessagesInterval.Registration", 10), register.timeout());
        int logging = Math.min(yaml.getInt("MessagesInterval.Logging", 10), login.timeout());

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
        int length = Math.max(8, Math.min(16, yaml.getInt("Captcha.Difficulty.Length", 8)));
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
        int memCost = yaml.getInt("Encryption.HashCost.MemoryCost", 1024);
        int parallelism = yaml.getInt("Encryption.HashCost.Parallelism", 22);
        int iterations = yaml.getInt("Encryption.HashCost.Iterations", 2);

        return CEncryptionSection.of(algorithm, encrypt, virtualID, memCost, parallelism, iterations);
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
        List<String> unlog = yaml.getStringList("Permissions.UnLogged");
        List<String> log = yaml.getStringList("Permissions.Logged");

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
        int length = yaml.getInt("Password.Safety.MinLength", 10);
        int chars = yaml.getInt("Password.Safety.Characters", 1);
        int numbers = yaml.getInt("Password.Safety.Numbers", 2);
        int upper = yaml.getInt("Password.Safety.Letters.Upper", 2);
        int lower = yaml.getInt("Password.Safety.Letters.Lower", 5);

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
    public BruteForceSection bruteForce() {
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
     * Get if the plugin enables the pin
     * login. Globally
     *
     * @return if the plugin uses pin
     */
    @Override
    public boolean enablePin() {
        return yaml.getBoolean("Pin", true);
    }

    /**
     * Get if the plugin enables the
     * 2fa login. Globally
     *
     * @return if the plugin uses 2fa
     */
    @Override
    public boolean enable2fa() {
        return yaml.getBoolean("2FA", true);
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
        int checkTime = yaml.getInt("Updater.CheckTime", 10);

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
            yaml.save(file.toFile(), (KarmaSource) CurrentPlugin.getPlugin(), "config.yml");
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
    public SpawnSection spawn() {
        boolean manage = yaml.getBoolean("Spawn.Manage", false);
        boolean back = yaml.getBoolean("Spawn.TakeBack", false);
        int distance = yaml.getInt("Spawn.SpawnDistance", 30);

        return CSpawnSection.of(manage, back, distance);
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
     * Get the plugin name check protocol
     *
     * @return the plugin name check protocl
     */
    @Override
    public int checkProtocol() {
        return yaml.getInt("NameCheckProtocol", 2);
    }

    /**
     * Get the plugin language
     *
     * @return the plugin language
     */
    @Override
    public String language() {
        return yaml.getString("Lang", "English");
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
        KarmaYamlManager instance = new KarmaYamlManager(serialized, false);
        yaml.update(instance, true, "ServerName", "SecretKey");
    }
}
