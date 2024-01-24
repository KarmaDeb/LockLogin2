package es.karmadev.locklogin.spigot.event.helper;

import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.file.section.CaptchaConfiguration;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.plugin.service.floodgate.FloodGateService;
import es.karmadev.locklogin.api.plugin.service.name.NameValidator;
import es.karmadev.locklogin.api.security.brute.BruteForceService;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class EventHelper {

    private static EventHelper instance;
    private final LockLoginSpigot spigot;

    private EventHelper(final LockLoginSpigot spigot) {
        this.spigot = spigot;
        instance = this;
    }

    private void internalGenerateCaptcha(final UserSession session) {
        Configuration configuration = spigot.configuration();

        if (!session.isCaptchaLogged()) {
            CaptchaConfiguration settings = configuration.captcha();
            if (settings.enable()) {
                int size = settings.length();
                String captcha = StringUtils.generateString(size);

                //String captcha = string.create();
                if (settings.strikethrough()) {
                    if (settings.randomStrike()) {
                        String last_color = ConsoleColor.lastColor(captcha);
                        StringBuilder builder = new StringBuilder();

                        SplittableRandom random = new SplittableRandom();
                        for (int i = 0; i < captcha.length(); i++) {
                            int val = random.nextInt(100);

                            if (val > 50) {
                                builder.append(last_color).append("&m").append(captcha.charAt(i)).append("&r");
                            } else {
                                builder.append(last_color).append(captcha.charAt(i)).append("&r");
                            }
                        }

                        captcha = builder.toString();
                    } else {
                        captcha = "&m" + captcha;
                    }
                }

                session.setCaptcha(captcha);
            } else {
                session.captchaLogin(true);
            }
        }
    }

    private boolean internalCheckMultiAccounts(final AsyncPlayerPreLoginEvent e, final InetAddress address, final LocalNetworkClient client, final Messages messages) {
        Configuration configuration = spigot.configuration();

        String name = e.getName();
        MultiAccountManager multi = spigot.accountManager();
        if (multi == null || multi.allow(address, configuration.register().maxAccounts())) {
            if (multi != null) {
                multi.assign(client, address);

                int amount = multi.getAccounts(address).size();
                int max = configuration.register().maxAccounts();
                if (amount >= max && max > 1) { //We only want to send a warning if the maximum amount of accounts is over 1
                    for (NetworkClient cl : spigot.network().getOnlinePlayers()) {
                        if (cl.hasPermission(LockLoginPermission.PERMISSION_INFO_ALT_ALERT)) {
                            cl.sendMessage(messages.prefix() + messages.altFound(name, amount));
                        }
                    }
                }
            }
        } else {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, Colorize.colorize(messages.maxRegisters()));
            return true;
        }

        return false;
    }

    private boolean internalCheckBan(final AsyncPlayerPreLoginEvent e, final InetAddress address, final Messages messages) {
        PluginService bf_service = spigot.getService("bruteforce");
        if (bf_service instanceof BruteForceService) {
            BruteForceService bruteforce = (BruteForceService) bf_service;

            if (bruteforce.isBlocked(address)) {
                long timeLeft = bruteforce.banTimeLeft(address);

                //BFAP = Brute Force Attack Protector
                spigot.logWarn("[BFAP] Address {0} tried to access the server but was blocked for brute force attack. Ban time remaining: {1}", address.getHostAddress(), TimeUnit.MILLISECONDS.toSeconds(timeLeft));

                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Colorize.colorize(messages.ipBlocked(timeLeft)));
                return true;
            }
        }

        return false;
    }

    private boolean internalCheckUUIDs(final AsyncPlayerPreLoginEvent e, final AtomicReference<UUID> use_uid, final Messages messages) {
        Configuration configuration = spigot.configuration();

        String name = e.getName();
        UUID provided_id = e.getUniqueId();
        if (configuration.verifyUniqueIDs()) {
            if (!provided_id.equals(use_uid.get())) {
                boolean deny = true;

                PluginService fg_service = spigot.getService("floodgate");
                if (fg_service instanceof FloodGateService) {
                    FloodGateService floodgate = (FloodGateService) fg_service;
                    if (floodgate.isBedrock(provided_id)) {
                        deny = false;
                        use_uid.set(provided_id);
                    }
                }

                if (deny) {
                    //USP = UUID Spoofer Protector
                    spigot.logWarn("[USP] Denied connection from {0} because its UUID ({1}) doesn't match with generated one ({2})", name, use_uid, provided_id);

                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Colorize.colorize(messages.uuidFetchError()));
                    return true;
                }
            }
        }

        return false;
    }

    private boolean internalCheckName(final AsyncPlayerPreLoginEvent e, final Messages messages) {
        String name = e.getName();
        UUID provided_id = e.getUniqueId();

        PluginService name_service = spigot.getService("name");
        if (name_service instanceof ServiceProvider) {
            ServiceProvider<? extends PluginService> provider = (ServiceProvider<?>) name_service;
            PluginService service = provider.serve(name);

            if (service instanceof NameValidator) {
                NameValidator validator = (NameValidator) service;
                validator.validate();

                if (validator.isValid()) {
                    spigot.logInfo("Successfully validated username of {0}", name);
                } else {
                    boolean deny = true;

                    PluginService fg_service = spigot.getService("floodgate");
                    if (fg_service instanceof FloodGateService) {
                        FloodGateService floodgate = (FloodGateService) fg_service;
                        if (floodgate.isBedrock(provided_id)) {
                            deny = false;
                            spigot.info("Connected bedrock client {0}", name);
                        }
                    }

                    if (deny) {
                        spigot.logWarn("[NVP] Denied connection from {0} because its name was not valid ({1})", name, ConsoleColor.strip(validator.invalidCharacters()));
                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Colorize.colorize(messages.illegalName(validator.invalidCharacters())));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean internalCheckOnline(final AsyncPlayerPreLoginEvent e, final AtomicReference<UUID> use_uid, final InetAddress address, final Messages messages) {
        Configuration configuration = spigot.configuration();

        Player online = Bukkit.getServer().getPlayer(use_uid.get());
        if (online != null && online.isOnline() && configuration.allowSameIp()) {
            InetSocketAddress online_address = online.getAddress();
            if (online_address != null && online_address.getAddress() != null) {
                InetAddress online_inet = online_address.getAddress();
                if (!Arrays.equals(online_inet.getAddress(), address.getAddress())) {
                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Colorize.colorize(messages.alreadyPlaying()));
                    return true;
                }
            }
        }

        return false;
    }

    public static PlayerEvent asPlayerEvent(final Player player, final EntityEvent event) {
        if (event instanceof Cancellable) {
            return new CancellablePE(player, (Cancellable) event);
        }

        return new SimplePE(player, event);
    }

    public static void setInstance(final LockLoginSpigot spigot) {
        new EventHelper(spigot);
    }

    public static void generateCaptcha(final UserSession session) {
        instance.internalGenerateCaptcha(session);
    }

    public static boolean checkMultiAccounts(final AsyncPlayerPreLoginEvent e, final String name, final InetAddress address, final LocalNetworkClient client, final Messages messages) {
        return instance.internalCheckMultiAccounts(e, address, client, messages);
    }

    public static boolean checkBan(final AsyncPlayerPreLoginEvent e, final InetAddress address, final Messages messages) {
        return instance.internalCheckBan(e, address, messages);
    }

    public static boolean checkUUID(final AsyncPlayerPreLoginEvent e, final AtomicReference<UUID> use_uid, final Messages messages) {
        return instance.internalCheckUUIDs(e, use_uid, messages);
    }

    public static boolean checkName(final AsyncPlayerPreLoginEvent e, final Messages messages) {
        return instance.internalCheckName(e, messages);
    }

    public static boolean checkOnline(final AsyncPlayerPreLoginEvent e, final AtomicReference<UUID> use_uid, final InetAddress address, final Messages messages) {
        return instance.internalCheckOnline(e, use_uid, address, messages);
    }
}
