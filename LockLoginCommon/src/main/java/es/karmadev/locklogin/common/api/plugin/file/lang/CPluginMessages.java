package es.karmadev.locklogin.common.api.plugin.file.lang;

import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.api.file.yaml.handler.YamlHandler;
import es.karmadev.api.file.yaml.handler.YamlReader;
import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.Alias;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.file.section.PasswordConfiguration;
import es.karmadev.locklogin.api.security.check.CheckResult;
import es.karmadev.locklogin.api.security.check.CheckType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Plugin messages
 */
public class CPluginMessages implements Messages {

    private final YamlFileHandler yaml;
    private final InternalPack parser;

    public CPluginMessages(final Path file, final InternalPack parser) {
        YamlFileHandler tmpYaml;
        String template;
        switch (parser.getName().toLowerCase()) {
            case "en_en":
            case "english":
                template = "messages_en.yml";
                break;
            default:
                template = "messages_template.yml";
        }

        InputStream stream = CurrentPlugin.getPlugin().loadResource("plugin/yaml/language/" + template);
        try {
            YamlReader reader = new YamlReader(stream);
            tmpYaml = YamlHandler.load(file, reader);
        } catch (IOException ex) {
            tmpYaml = YamlHandler.create(file);
        }

        this.yaml = tmpYaml;
        yaml.validate();

        this.parser = parser;
    }

    /**
     * Reload the messages file
     */
    @Override
    public boolean reload() {
        return yaml.reload();
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String prefix() {
        return parser.parse(yaml.getString("Prefix", "&eLockLogin &7>> "));
    }

    /**
     * Get a plugin message
     *
     * @param entity message replace
     * @return plugin message
     */
    @Override
    public String join(final NetworkClient entity) {
        String str = yaml.getString("PlayerJoin", "&8&l[&e&o+&8&l] &f{player}");
        return parser.parse(str.replace("{player}", entity.name()));
    }

    /**
     * Get a plugin message
     *
     * @param entity message replace
     * @return plugin message
     */
    @Override
    public String leave(final NetworkClient entity) {
        String str = yaml.getString("PlayerLeave", "&8&l[&3&o-&8&l] &f{player}");
        return parser.parse(str.replace("{player}", entity.name()));
    }

    /**
     * Get a plugin message for modules
     *
     * @param permission message replace
     * @return plugin module message
     */
    @Override
    public String permissionError(final PermissionObject permission) {
        String str = yaml.getString("PermissionError", "&5&oYou do not have the permission {permission}");

        return parser.parse(str.replace("{permission}", permission.node()));
    }

    /**
     * Get a plugin message
     *
     * @return the plugin message
     */
    @Override
    public String completeExtra() {
        return parser.parse(yaml.getString("CompleteExtra", "&5&oPlease, complete the extra authentication steps"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String bungeeProxy() {
        return parser.parse(yaml.getString("BungeeProxy", "&5&oPlease, connect through bungee proxy!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String pinTitle() {
        return parser.parse(yaml.getString("PinTitle", "&eLockLogin pinner"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String altTitle() {
        return parser.parse(yaml.getString("AltTitle", "&8&lAlt accounts lookup"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String infoTitle() {
        return parser.parse(yaml.getString("InfoTitle", "&8&lBundled player info"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String nextButton() {
        return parser.parse(yaml.getString("Next", "&eNext"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String backButton() {
        return parser.parse(yaml.getString("Back", "&eBack"));
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    @Override
    public String notVerified(final NetworkEntity target) {
        String str = yaml.getString("PlayerNotVerified", "&5&oYou can't fight against {player} while he's not logged/registered");

        return parser.parse(str.replace("{player}", target.name()));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String alreadyPlaying() {
        return parser.parse(yaml.getString("AlreadyPlaying", "&5&oThat player is already playing"));
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    @Override
    public String connectionError(final String name) {
        String str = yaml.getString("ConnectionError", "&5&oThe player {player} is not online");

        return parser.parse(str.replace("{player}", name));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String infoUsage() {
        return parser.parse(yaml.getString("PlayerInfoUsage", "&5&oPlease, use /playerinfo <player>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String lookupUsage() {
        return parser.parse(yaml.getString("LookUpUsage", "&5&oPlease, use /lookup <player>"));
    }

    /**
     * Get a plugin message
     *
     * @param name   message replace
     * @param amount message replace
     * @return plugin message
     */
    @Override
    public String altFound(final String name, final int amount) {
        String str = yaml.getString("AltFound", "&5&o{player} could have {alts} alt accounts, type /lookup {player} for more info");

        return parser.parse(str.replace("{player}", name).replace("{alts}", String.valueOf(amount)));
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    @Override
    public String neverPlayer(final String name) {
        String str = yaml.getString("NeverPlayed", "&5&oThe player {player} never played on the server");

        return parser.parse(str.replace("{player}", name));
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    @Override
    public String targetAccessError(final String name) {
        String str = yaml.getString("TargetAccessError", "&5&oThe player {player} isn't logged in/registered");

        return parser.parse(str.replace("{player}", name));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String incorrectPassword() {
        return parser.parse(yaml.getString("IncorrectPassword", "&5&oThe provided password is not correct!"));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @return plugin message
     */
    @Override
    public String captcha(String code) {
        if (code == null) code = "";

        String str = yaml.getString("Captcha", "&7Your captcha code is: &e{captcha}");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String invalidCaptcha() {
        return parser.parse(yaml.getString("InvalidCaptcha", "&5&oThe specified captcha code is not valid or correct"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String sessionServerDisabled() {
        return parser.parse(yaml.getString("SessionServerDisabled", "&5&oPersistent sessions are disabled in this server"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String sessionEnabled() {
        return parser.parse(yaml.getString("SessionEnabled", "&dEnabled persistent session for your account ( &e-security&c )"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String sessionDisabled() {
        return parser.parse(yaml.getString("SessionDisabled", "&5&oDisabled persistent session for your account ( &e+security&c )"));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @return plugin message
     */
    @Override
    public String register(String code) {
        if (code == null) code = "";

        String str = yaml.getString("Register", "&5&oPlease, use /register <password> <password> <captcha>");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param color message replace
     * @param time  message replace
     * @return plugin message
     */
    @Override
    public String registerBar(String code, final String color, final long time) {
        if (code == null) code = "";

        String str = yaml.getString("RegisterBar", "{color}You have &7{time}{color} to register");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + ConsoleColor.strip("second(s)");
        } else {
            if (minutes <= 59) {
                format = minutes + " " + ConsoleColor.strip("minute(s)") + " and " + Math.abs((minutes * 60) - time) + " " + ConsoleColor.strip("sec(s)");
            } else {
                format = hours + " " + ConsoleColor.strip("hour(s)") + " and " + Math.abs((hours * 60) - minutes) + " " + ConsoleColor.strip("min(s)");
            }
        }
        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{color}", color)
                .replace("{time}", format)
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String registered() {
        return parser.parse(yaml.getString("Registered", "&dRegister completed, thank for playing"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String alreadyRegistered() {
        return parser.parse(yaml.getString("AlreadyRegistered", "&5&oYou are already registered!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String registerError() {
        return parser.parse(yaml.getString("RegisterError", "&5&oThe provided passwords does not match!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String passwordInsecure() {
        return parser.parse(yaml.getString("PasswordInsecure", "&5&oThe specified password is not secure!"));
    }

    /**
     * Get a plugin messages
     *
     * @param client message replace
     * @return plugin message
     */
    @Override
    public String passwordWarning(final NetworkClient client) {
        String str = yaml.getString("PasswordWarning", "&5&oThe client {player} is using an unsafe password");

        return parser.parse(str.replace("{player}", client.name()));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String registerTimeOut() {
        return parser.parse(yaml.getString("RegisterOut", "&5&oYou took too much time to register"));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param time message replace
     * @return plugin message
     */
    @Override
    public String registerTitle(String code, final long time) {
        if (code == null) code = "";

        String str = yaml.getString("RegisterTitle", "&7You have");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + ConsoleColor.strip("second(s)");
        } else {
            if (minutes <= 59) {
                format = minutes + " " + ConsoleColor.strip("minute(s)") + " and " + Math.abs((minutes * 60) - time) + " " + ConsoleColor.strip("sec(s)");
            } else {
                format = hours + " " + ConsoleColor.strip("hour(s)") + " and " + Math.abs((hours * 60) - minutes) + " " + ConsoleColor.strip("min(s)");
            }
        }
        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{time}", format)
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param time message replace
     * @return plugin message
     */
    @Override
    public String registerSubtitle(String code, final long time) {
        if (code == null) code = "";

        String str = yaml.getString("RegisterSubtitle", "&b{time} &7to register");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + ConsoleColor.strip("second(s)");
        } else {
            if (minutes <= 59) {
                format = minutes + " " + ConsoleColor.strip("minute(s)") + " and " + Math.abs((minutes * 60) - time) + " " + ConsoleColor.strip("sec(s)");
            } else {
                format = hours + " " + ConsoleColor.strip("hour(s)") + " and " + Math.abs((hours * 60) - minutes) + " " + ConsoleColor.strip("min(s)");
            }
        }
        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{time}", format)
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String maxRegisters() {
        List<String> messages = yaml.getList("MaxRegisters");
        StringBuilder builder = new StringBuilder();

        Configuration config = CurrentPlugin.getPlugin().configuration();
        for (String str : messages) {
            builder.append(str
                    .replace("{ServerName}", config.server())).append("\n");
        }

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @return plugin message
     */
    @Override
    public String login(String code) {
        if (code == null) code = "";

        String str = yaml.getString("Login", "&5&oPlease, use /login <password> <captcha>");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param color message replace
     * @param time  message replace
     * @return plugin message
     */
    @Override
    public String loginBar(String code, final String color, final long time) {
        if (code == null) code = "";

        String str = yaml.getString("LoginBar", "{color}You have &7{time}{color} to login");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + ConsoleColor.strip("second(s)");
        } else {
            if (minutes <= 59) {
                format = minutes + " " + ConsoleColor.strip("minute(s)") + " and " + Math.abs((minutes * 60) - time) + " " + ConsoleColor.strip("sec(s)");
            } else {
                format = hours + " " + ConsoleColor.strip("hour(s)") + " and " + Math.abs((hours * 60) - minutes) + " " + ConsoleColor.strip("min(s)");
            }
        }
        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{color}", color)
                .replace("{time}", format)
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String logged() {
        return parser.parse(yaml.getString("Logged", "&dLogged-in, welcome back &7{player}"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String alreadyLogged() {
        return parser.parse(yaml.getString("AlreadyLogged", "&5&oYou are already logged!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String loginInsecure() {
        return parser.parse(yaml.getString("LoginInsecure", "&5&oYour password has been classified as not secure and you must change it"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String loginTimeOut() {
        return parser.parse(yaml.getString("LoginOut", "&5&oYou took too much time to log-in"));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param time message replace
     * @return plugin message
     */
    @Override
    public String loginTitle(String code, final long time) {
        if (code == null) code = "";

        String str = yaml.getString("RegisterTitle", "&7You have");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + ConsoleColor.strip("second(s)");
        } else {
            if (minutes <= 59) {
                format = minutes + " " + ConsoleColor.strip("minute(s)") + " and " + Math.abs((minutes * 60) - time) + " " + ConsoleColor.strip("sec(s)");
            } else {
                format = hours + " " + ConsoleColor.strip("hour(s)") + " and " + Math.abs((hours * 60) - minutes) + " " + ConsoleColor.strip("min(s)");
            }
        }
        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{time}", format)
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @param time message replace
     * @return plugin message
     */
    @Override
    public String loginSubtitle(String code, final long time) {
        if (code == null) code = "";

        String str = yaml.getString("LoginSubtitle", "&b{time} &7to login");
        Configuration config = CurrentPlugin.getPlugin().configuration();

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + ConsoleColor.strip("second(s)");
        } else {
            if (minutes <= 59) {
                format = minutes + " " + ConsoleColor.strip("minute(s)") + " and " + Math.abs((minutes * 60) - time) + " " + ConsoleColor.strip("sec(s)");
            } else {
                format = hours + " " + ConsoleColor.strip("hour(s)") + " and " + Math.abs((hours * 60) - minutes) + " " + ConsoleColor.strip("min(s)");
            }
        }
        String captcha = code;
        if (config.captcha().strikethrough()) {
            if (config.captcha().randomStrike()) {
                String last_color = ConsoleColor.lastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                captcha = builder.toString();
            } else {
                captcha = "&m" + code;
            }
        }
        if (captcha.isEmpty()) {
            str = str.replace("<captcha>", "");
        }

        return parser.parse(str
                .replace("{time}", format)
                .replace("{captcha}", captcha)
                .replace("<captcha>", captcha));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String loginForgot() {
        return yaml.getString("LoginForgot", "&5&oIt seems that you've forgot your password, do you want us to send a recovery email for you?");
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String loginForgotYes() {
        return yaml.getString("LoginForgotYes", "&dYes please");
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String loginForgotNo() {
        return yaml.getString("LoginForgotNo", "&5&oNo, thanks");
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumEnabled() {
        List<String> messages = yaml.getList("PremiumEnabled");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumDisabled() {
        List<String> messages = yaml.getList("PremiumDisabled");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");
        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumError() {
        return parser.parse(yaml.getString("PremiumError", "&5&oAn error occurred while disabling/enabling premium in your account"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumAuth() {
        return parser.parse(yaml.getString("PremiumAuth", "&dAuthenticated using premium authentication"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumServer() {
        return parser.parse(yaml.getString("PremiumServer", "&5&lThis server is for premium users only!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumWarning() {
        List<String> messages = yaml.getList("PremiumWarning");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumFailAuth() {
        return parser.parse(yaml.getString("PremiumFails.AuthenticationError", "&5&oFailed to validate your connection. Are you authenticated through mojang?"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumFailInternal() {
        return parser.parse(yaml.getString("PremiumFails.InternalError", "&5&oAn internal server error occurred"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumFailConnection() {
        return parser.parse(yaml.getString("PremiumFails.ConnectionError", "&5&oFailed to validate your connection with this server"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumFailAddress() {
        return parser.parse(yaml.getString("AddressError", "&5&oFailed to authenticate your address"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumFailEncryption() {
        return parser.parse(yaml.getString("PremiumFails.EncryptionError", "&5&oInvalid encryption provided"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumFailPrecocious() {
        return parser.parse(yaml.getString("PremiumFails.PrecociousEncryption", "&5&oTried to authenticate before the server does!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String premiumFailSession() {
        return parser.parse(yaml.getString("PremiumFails.SessionError", "&5&oInvalid session"));
    }

    /**
     * Get a plugin message
     *
     * @param result message argument
     * @return plugin message
     */
    @Override
    public String checkResult(final CheckResult result) {
        if (result.valid()) return "";

        StringBuilder builder = new StringBuilder();
        Configuration config = CurrentPlugin.getPlugin().configuration();
        PasswordConfiguration passwordConfig = config.password();

        for (CheckType type : CheckType.values()) {
            String str = null;
            if (result.getStatus(type)) {
                if (passwordConfig.printSuccess()) str = yaml.getString("Password.Success." + type.name, type.successMessage);
            } else {
                str = yaml.getString("Password.Failed." + type.name, type.failMessage);
            }

            if (str == null) continue;
            builder.append(str
                    .replace("{min_length}", String.valueOf(passwordConfig.minLength()))
                    .replace("{min_special}", String.valueOf(passwordConfig.characters()))
                    .replace("{min_number}", String.valueOf(passwordConfig.numbers()))
                    .replace("{min_lower}", String.valueOf(passwordConfig.lowerLetters()))
                    .replace("{min_upper}", String.valueOf(passwordConfig.upperLetters()))
                    .replace("{length}", String.valueOf(result.length()))
                    .replace("{special}", String.valueOf(result.specialCharacters()))
                    .replace("{number}", String.valueOf(result.numbers()))
                    .replace("{lower}", String.valueOf(result.lowerLetters()))
                    .replace("{upper}", String.valueOf(result.upperLetters()))).append("\n");
        }

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String pinUsages() {
        return parser.parse(yaml.getString("PinUsages", "&5&oValid pin sub-arguments: &e<setup>&7, &e<remove>&7, &e<change>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String pinSet() {
        return parser.parse(yaml.getString("PinSet", "&dYour pin has been set successfully"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String pinReseted() {
        return parser.parse(yaml.getString("PinReseted", "&5&oPin removed, your account is now less secure"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String pinChanged() {
        return parser.parse(yaml.getString("PinChanged", "&dYour pin has been changed successfully"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String alreadyPin() {
        return parser.parse(yaml.getString("AlreadyPin", "&5&oYou already have set your pin!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String noPin() {
        return parser.parse(yaml.getString("NoPin", "&5&oYou don't have a pin!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String setPin() {
        return parser.parse(yaml.getString("SetPin", "&5&oPlease, use /pin setup <pin>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String resetPin() {
        return parser.parse(yaml.getString("ResetPin", "&5&oPlease, use /pin reset <pin>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String changePin() {
        return parser.parse(yaml.getString("ChangePin", "&5&oPlease, use /pin change <pin> <new pin>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String pinDisabled() {
        return parser.parse(yaml.getString("PinDisabled", "&5&oPins are disabled"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String pinLength() {
        return parser.parse(yaml.getString("PinLength", "&5&oPin must have 4 digits"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String incorrectPin() {
        return parser.parse(yaml.getString("IncorrectPin", "&5&oThe specified pin is not correct!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthUsages() {
        return parser.parse(yaml.getString("2FaUsages", "&5&oValid TOTP sub-arguments: &e<setup>&7, &e<remove>&7, &e<TOTP code>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthSetupUsage() {
        return parser.parse(yaml.getString("2FaSetupUsage", "&5&oPlease, use /totp setup <password>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthRemoveUsage() {
        return parser.parse(yaml.getString("2FaRemoveUsage", "&5&oPlease, use /totp remove <password> <TOTP code>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthDisableUsage() {
        return parser.parse(yaml.getString("2FaDisableUsage", "&5&oPlease, use /totp disable <password> <TOTP code>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthRequired() {
        return parser.parse(yaml.getString("2FaAuthenticate", "&5&oPlease, use /totp to start playing"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthCorrect() {
        return parser.parse(yaml.getString("2FaLogged", "&dTOTP code validated"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthAlready() {
        return parser.parse(yaml.getString("2FaAlreadyLogged", "&5&oYou are already authenticated with TOTP!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthIncorrect() {
        return parser.parse(yaml.getString("2FaIncorrect", "&5&oIncorrect TOTP code"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthSetupAlready() {
        return parser.parse(yaml.getString("2FaAlready", "&5&oYou already have setup your TOTP!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthToggleError() {
        return parser.parse(yaml.getString("ToggleFAError", "&5&oError while trying to toggle TOTP ( incorrect password/code )"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthDisabled() {
        return parser.parse(yaml.getString("Disabled2FA", "&5&oTOTP disabled, your account is now less secure"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthEnabled() {
        return parser.parse(yaml.getString("Enabled2FA", "&dTOTP enabled, your account is secure again"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthNotEnabled() {
        return parser.parse(yaml.getString("2FaAccountDisabled", "&5&oTOTP is disabled in your account"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthServerDisabled() {
        return parser.parse(yaml.getString("2FAServerDisabled", "&5&oTOTP is currently disabled in this server"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gauthLocked() {
        return parser.parse(yaml.getString("2FaLocked", "&5&oThis server wants you to have TOTP enabled"));
    }

    /**
     * Get a plugin message
     *
     * @param codes message replace
     * @return plugin message
     */
    @Override
    public String gAuthScratchCodes(final List<String> codes) {
        List<String> messages = yaml.getList("2FAScratchCodes");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        builder.append("\n&r ");

        int added = 0;
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            builder.append("&e").append(code).append((added == 2 ? "\n" : (i != codes.size() - 1 ? "&7, " : "")));
            added++;
        }

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthInstructions() {
        List<String> messages = yaml.getList("2FaInstructions");
        StringBuilder builder = new StringBuilder();

        Configuration config = CurrentPlugin.getPlugin().configuration();

        for (String str : messages)
            builder.append(str
                    .replace("{message}", gAuthLink() + ConsoleColor.lastColor(str))
                    .replace("{account}", "{player} (" + config.server() + ")")).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String gAuthLink() {
        return parser.parse(yaml.getString("2FaLink", "&bClick here to get your TOTP QR code"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String panicLogin() {
        return parser.parse(yaml.getString("PanicLogin", "&5&oPlease, use /panic <token>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String panicTitle() {
        return parser.parse(yaml.getString("PanicTitle", "&cPANIC MODE"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String panicSubtitle() {
        return parser.parse(yaml.getString("PanicSubtitle", "&cRUN &4/panic <token>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String panicMode() {
        return parser.parse(yaml.getString("PanicMode", "&5&oThe account entered in panic mode, you have 1 token login try before being IP-blocked"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String panicDisabled() {
        return parser.parse(yaml.getString("PanicDisabled", "&5&oThe server is not protected against brute force attacks"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String panicAlready() {
        return parser.parse(yaml.getString("PanicAlready", "&dYou already have a brute force token, your account is secure"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String panicRequested() {
        List<String> messages = yaml.getList("PanicRequested");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String panicEnabled() {
        List<String> messages = yaml.getList("PanicEnabled");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String tokenLink() {
        return parser.parse(yaml.getString("TokenLink", "&bClick to reveal the token"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String accountArguments() {
        return parser.parse(yaml.getString("AccountArguments", "&5&oValid account sub-arguments: &e<change>&7, &e<unlock>&7, &e<close>&7, &e<remove>&7, &e<protect>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String change() {
        return parser.parse(yaml.getString("Change", "&5&oPlease, use /account change <password> <new password>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String changeSame() {
        return parser.parse(yaml.getString("ChangeSame", "&5&oYour password can't be the same as old!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String changeDone() {
        return parser.parse(yaml.getString("ChangeDone", "&dYour password has changed!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String accountUnLock() {
        return parser.parse(yaml.getString("AccountUnlock", "&5&oPlease, use /account unlock <player>"));
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    @Override
    public String accountUnLocked(final String target) {
        String str = yaml.getString("AccountUnlocked", "&dAccount of {player} has been unlocked");

        return parser.parse(str.replace("{player}", target));
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    @Override
    public String accountNotLocked(final String target) {
        String str = yaml.getString("AccountNotLocked", "&5&oAccount of {player} is not locked!");

        return parser.parse(str.replace("{player}", target));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String close() {
        return parser.parse(yaml.getString("Close", "&5&oPlease, use /account close [player]"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String closed() {
        return parser.parse(yaml.getString("Closed", "&5&oSession closed, re-login now!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String forcedClose() {
        return parser.parse(yaml.getString("ForcedClose", "&5&oYour session have been closed by an admin, login again"));
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    @Override
    public String forcedCloseAdmin(final NetworkEntity target) {
        String str = yaml.getString("ForcedCloseAdmin", "&dSession of {player} closed");

        return parser.parse(str.replace("{player}", target.name()));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String remove() {
        return parser.parse(yaml.getString("Remove", "&5&oPlease, use /account remove <password|player> [password]"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String removeAccountMatch() {
        return parser.parse(yaml.getString("RemoveAccountMatch", "&5&oThe provided passwords does not match"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String accountRemoved() {
        return parser.parse(yaml.getString("AccountRemoved", "&5&oYour account have been deleted"));
    }

    /**
     * Get a plugin message
     *
     * @param administrator message replace
     * @return plugin message
     */
    @Override
    public String forcedAccountRemoval(final String administrator) {
        List<String> messages = yaml.getList("ForcedAccountRemoval");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str
                    .replace("{player}", ConsoleColor.strip(administrator))).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    @Override
    public String forcedAccountRemovalAdmin(final String target) {
        String str = yaml.getString("ForcedAccountRemovalAdmin", "&dAccount of {player} removed, don't forget to run /account unlock {player}!");

        return parser.parse(str.replace("{player}", target));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnUsage() {
        return parser.parse(yaml.getString("SpawnUsage", "&5&oValid spawn sub-arguments: &e<set>&7, &e<unset>&7, &e<teleport>&7, &e<back>"));
    }

    /**
     * Get a plugin message
     *
     * @param rawLocation message replace
     * @return plugin message
     */
    @Override
    public String spawnSet(final String rawLocation) {
        String str = yaml.getString("SpawnSet", "&dThe login spawn location have been set at&7 {position}");
        return parser.parse(str.replace("{position}", rawLocation));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnNotSet() {
        return parser.parse(yaml.getString("SpawnNotSet", "&5&oThe spawn location has not been set!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnUnset() {
        return parser.parse(yaml.getString("SpawnUnset", "&dThe login spawn location has been unset"));
    }

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    @Override
    public String spawnTeleportTime(final int time) {
        String str = yaml.getString("SpawnTeleportTime", "&dYou will be teleported to spawn in&7 {time}&d seconds");

        return parser.parse(str.replace("{time}", String.valueOf(time)));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnTeleport() {
        return parser.parse(yaml.getString("SpawnTeleport", "&dYou've been teleported to spawn"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnTeleportAdmin() {
        return parser.parse(yaml.getString("SpawnTeleportAdmin", "&dYou've been teleported to spawn, run &7/spawn back&d to go to your previous location"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnTeleportNear() {
        return parser.parse(yaml.getString("SpawnTeleportNear", "&5&oYou are too nearby to spawn!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnCancelled() {
        return parser.parse(yaml.getString("SpawnCancelled", "&5&oThe spawn teleportation has been cancelled"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnBack() {
        return parser.parse(yaml.getString("SpawnBack", "&dYou've been teleported to your previous location"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String spawnNoBack() {
        return parser.parse(yaml.getString("SpawnNoBack", "&5&oYou don't have a previous location to go at"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String locationsReset() {
        return parser.parse(yaml.getString("LocationsReset", "&dAll last locations have been reset"));
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    @Override
    public String locationReset(final String name) {
        String str = yaml.getString("LocationReset", "&dLast location of {player} has been reset");

        return parser.parse(str.replace("{player}", name));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String locationsFixed() {
        return parser.parse(yaml.getString("LocationsFixed", "&dAll last locations have been fixed"));
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    @Override
    public String locationFixed(final String name) {
        String str = yaml.getString("LocationFixed", "&dLocation of {player} has been fixed");

        return parser.parse(str.replace("{player}", name));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String resetLocUsage() {
        return parser.parse(yaml.getString("ResetLastLocUsage", "&5&oPlease, use /locations [player|@all|@me] <remove|fix|go>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String alias() {
        return parser.parse(yaml.getString("AliasArguments", "&5&oValid alias sub-arguments: &e<create>&7, &e<destroy>&7, &e<add>&7, &e<remove> [alias] [player(s)]"));
    }

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    @Override
    public String aliasCreated(final Alias alias) {
        String str = yaml.getString("AliasCreated", "&dAlias {alias} created successfully");

        return parser.parse(str.replace("{alias}", alias.name()));
    }

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    @Override
    public String aliasDestroyed(final Alias alias) {
        String str = yaml.getString("AliasDestroyed", "&5&oAlias {alias} has been destroyed");

        return parser.parse(str.replace("{alias}", alias.name()));
    }

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    @Override
    public String aliasExists(final Alias alias) {
        String str = yaml.getString("AliasExists", "&5&oAlias {alias} already exists!");

        return parser.parse(str.replace("{alias}", alias.name()));
    }

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    @Override
    public String aliasNotFound(final String alias) {
        String str = yaml.getString("AliasNotFound", "&5&oCouldn't find any alias called {alias}");

        return parser.parse(str.replace("{alias}", alias.toUpperCase().replace(" ", "_")));
    }

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    @Override
    public String addedPlayer(final Alias alias, final String... players) {
        String str = yaml.getString("AddedPlayer", "&dAdded {player} to {alias}");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parser.parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.name()));
    }

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    @Override
    public String removedPlayer(final Alias alias, final String... players) {
        String str = yaml.getString("RemovedPlayer", "&dRemoved {player} from {alias}");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parser.parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.name()));
    }

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    @Override
    public String playerNotIn(final Alias alias, final String... players) {
        String str = yaml.getString("PlayerNotIn", "&5&o{player} is not in {alias}!");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parser.parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.name()));
    }

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    @Override
    public String playerAlreadyIn(final Alias alias, final String... players) {
        String str = yaml.getString("PlayerAlreadyIn", "&5&o{player} is already in {alias}!");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parser.parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.name()));
    }

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    @Override
    public String ipBlocked(final long time) {
        List<String> messages = yaml.getList("IpBlocked");
        StringBuilder builder = new StringBuilder();

        long seconds = TimeUnit.SECONDS.toSeconds(time);
        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        long final_time;
        if (seconds <= 59) {
            format = "sec(s)";
            final_time = seconds;
        } else {
            if (minutes <= 59) {
                format = "min(s) and " + Math.abs((minutes * 60) - seconds) + " sec(s)";
                final_time = minutes;
            } else {
                format = "hour(s) " + Math.abs((hours * 60) - minutes) + " min(s)";
                final_time = hours;
            }
        }

        for (String str : messages) {
            builder.append(str
                    .replace("{time}", String.valueOf(final_time))
                    .replace("{time_format}", format)).append("\n");
        }

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @param chars message replace
     * @return plugin message
     */
    @Override
    public String illegalName(final String chars) {
        List<String> messages = yaml.getList("IllegalName");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str.replace("{chars}", chars)).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @param name       messages replace
     * @param knownNames message replace
     * @return plugin message
     */
    @Override
    public String multipleNames(final String name, final String... knownNames) {
        List<String> messages = yaml.getList("MultipleNames");
        StringBuilder nameBuilder = new StringBuilder();

        for (String known : knownNames)
            nameBuilder.append("&d").append(known).append("&5, ");

        String names = StringUtils.replaceLast(nameBuilder.toString(), "&5, ", "");

        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str.replace("{player}", name).replace("{players}", names)).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String uuidFetchError() {
        List<String> messages = yaml.getList("UUIDFetchError");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String ipProxyError() {
        List<String> messages = yaml.getList("IpProxyError");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @Override
    public String bedrockJava() {
        List<String> messages = yaml.getList("BedrockJavaError");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parser.parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Serialize the messages
     *
     * @return the serialized messages
     */
    @Override
    public String serialize() {
        return yaml.toString();
    }

    /**
     * Load the messages
     *
     * @param serialized the serialized messages
     */
    @Override
    public void load(final String serialized) {
        YamlFileHandler instance = YamlHandler.load(serialized);
        yaml.importFrom(instance, false);
    }
}