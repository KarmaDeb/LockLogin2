package es.karmadev.locklogin.spigot.event;

import es.karmadev.api.strings.StringOptions;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.network.TextContainer;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.plugin.marketplace.MarketPlace;
import es.karmadev.locklogin.api.plugin.marketplace.resource.MarketResource;
import es.karmadev.locklogin.api.plugin.marketplace.resource.ResourceDownload;
import es.karmadev.locklogin.api.plugin.marketplace.storage.ResourceManager;
import es.karmadev.locklogin.api.plugin.marketplace.storage.StoredResource;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.mail.EmailService;
import es.karmadev.locklogin.api.plugin.service.mail.MailMessage;
import es.karmadev.locklogin.api.security.brute.BruteForceService;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.plugin.service.mail.CMailMessage;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;
import es.karmadev.locklogin.common.plugin.secure.CommandWhitelist;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.window.pin.PinInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ChatHandler implements Listener {

    private final LockLoginSpigot spigot;
    private final Map<TextContainer, MarketResource> downloadQue = new HashMap<>();

    private boolean runningLRM = false;

    public ChatHandler(final LockLoginSpigot spigot) {
        this.spigot = spigot;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        int id = UserDataHandler.getNetworkId(player);

        if (id >= 0) {
            NetworkClient client = spigot.network().getPlayer(id);
            UserSession session = client.session();

            e.setCancelled(!session.isLogged() || !session.isPinLogged() || !session.isTotpLogged());
        } else {
            if (UserDataHandler.isReady(player)) {
                e.setCancelled(true);
            }
        }

        if (!e.isCancelled() && id >= 0) {
            NetworkClient client = spigot.network().getPlayer(id);

            String message = e.getMessage();
            if (message.startsWith("lrm") && client.hasPermission(LockLoginPermission.PERMISSION_MODULE)) {
                delegateToLRM(client, message);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        Messages messages = spigot.messages();

        int id = UserDataHandler.getNetworkId(player);

        if (id >= 0) {
            NetworkClient client = spigot.network().getPlayer(id);
            UserSession session = client.session();

            String message = e.getMessage();

            String command = parseCommand(message);
            String[] arguments = parseArguments(message);

            if (arguments.length == 1) {
                String code = arguments[0];
                switch (command) {
                    case "kickme":
                        String kickCode = session.fetch("kick_code", null);
                        if (kickCode != null && kickCode.equals(code)) {
                            InetSocketAddress address = player.getAddress();
                            if (address == null) {
                                client.kick(messages.ipProxyError());

                                session.append(CSessionField.newField(String.class, "mail_code", null));
                                session.append(CSessionField.newField(String.class, "kick_code", null));
                                return;
                            }

                            PluginService service = spigot.getService("bruteforce");
                            if (service instanceof BruteForceService) {
                                BruteForceService bruteForce = (BruteForceService) service;

                                if (client.account().isProtected()) {
                                    bruteForce.success(address.getAddress()); //We use this to "clear" tries count and allow the "final panic try"
                                    client.kick(messages.panicLogin());
                                    bruteForce.togglePanic(client, true); //We are now panicking

                                    session.append(CSessionField.newField(String.class, "mail_code", null));
                                    session.append(CSessionField.newField(String.class, "kick_code", null));
                                    return;
                                }

                                client.kick(messages.incorrectPassword());
                                bruteForce.fail(address.getAddress());

                                session.append(CSessionField.newField(String.class, "mail_code", null));
                                session.append(CSessionField.newField(String.class, "kick_code", null));
                                return;
                            }

                            client.kick("&4&l[ERROR at ChatHandler#84]");
                        }
                        break;
                    case "mailme":
                        String mailCode = session.fetch("mail_code", null);
                        if (mailCode != null && mailCode.equals(code)) {
                            String recoveryCode = StringUtils.generateString(8, StringOptions.UPPERCASE, StringOptions.NUMBERS);
                            session.append(CSessionField.newField(String.class, "recovery_code", recoveryCode));

                            Path template = spigot.workingDirectory().resolve("templates").resolve("mailer").resolve("forgot_password.html");
                            if (!Files.exists(template)) {
                                spigot.plugin().export("plugin/html/forgot_password.html", template);
                            }

                            MailMessage mailMessage = CMailMessage.builder(template.toFile())
                                    .origin("noreply@karmadev.es")
                                    .subject("Your password recovery code")
                                    .applyPlaceholder("player", client.name())
                                    .applyPlaceholder("code", recoveryCode)
                                    .build();

                            PluginService service = spigot.getService("mailer");
                            if (service instanceof EmailService) {
                                EmailService email = (EmailService) service;
                                email.send(client, mailMessage);

                                session.append(CSessionField.newField(String.class, "mail_code", null));
                                session.append(CSessionField.newField(String.class, "kick_code", null));
                            }
                        }
                        break;
                }
            }

            boolean pwdLogged = session.fetch("pass_logged", false);
            boolean pinLogged = session.fetch("pin_logged", false);
            boolean totLogged = session.fetch("totp_logged", false);

            if (!pwdLogged || !pinLogged || !totLogged) {
                if (CommandMask.mustMask(command)) {
                    UUID commandId = CommandMask.mask(message, arguments);

                    String masked = CommandMask.getCommand(commandId) + " " + commandId;
                    e.setMessage(masked);
                }

                if (CommandWhitelist.isBlacklisted(command)) {
                    e.setCancelled(true);
                    if (!pwdLogged) {
                        if (client.account().isRegistered()) {
                            client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                        } else {
                            client.sendMessage(messages.prefix() + messages.register(session.captcha()));
                        }

                        return;
                    }

                    if (!pinLogged && client.account().hasPin()) {
                        if (client.account().hasPin()) {
                            client.sendMessage(messages.prefix() + messages.incorrectPin());

                            PinInventory inventory = PinInventory.getInstance(client);
                            if (inventory != null) {
                                Inventory i = inventory.getInventory();
                                player.openInventory(i);
                            }
                        } else {
                            client.sendMessage(messages.prefix() + "&cComplete the extra login steps");
                        }
                        return;
                    }

                    if (!totLogged && client.account().hasTotp()) {
                        client.sendMessage(messages.prefix() + messages.gAuthRequired());
                        return;
                    }

                    client.sendMessage(messages.prefix() + messages.completeExtra());
                }
            }
        } else {
            if (UserDataHandler.isReady(player)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsoleCommand(ServerCommandEvent e) {
        String message = e.getCommand();

        String command = parseCommand(message);
        if (command.equalsIgnoreCase("locklogin") && spigot.bungeeMode()) {
            e.setCommand(message.replaceFirst("locklogin", "spigot-locklogin"));
        }

        if (message.startsWith("lrm")) {
            delegateToLRM(spigot, message);
            e.setCancelled(true);
        }
    }

    private String parseCommand(final String command) {
        String parseTarget = command;
        if (command.contains(" ")) {
            String[] data = command.split(" ");
            parseTarget = data[0];
        }

        if (parseTarget.contains(":")) {
            String[] data = parseTarget.split(":");
            String plugin = data[0];
            parseTarget = parseTarget.replaceFirst(plugin + ":", "");
        }

        return parseTarget;
    }

    private String[] parseArguments(final String command) {
        List<String> arguments = new ArrayList<>();
        if (command.contains(" ")) {
            String[] data = command.split(" ");
            if (data.length >= 1) {
                arguments.addAll(Arrays.asList(Arrays.copyOfRange(data, 1, data.length)));
            }
        }

        return arguments.toArray(new String[0]);
    }

    private void delegateToLRM(final TextContainer client, final String command) {
        if (!command.contains(" ")) {
            client.sendMessage("&cInvalid LockLogin resource manager command; Expecting <argument> but got nothing");
            client.sendMessage("&cRun&7 lrm help&c for help");
            return;
        }

        String[] data = command.split(" ");
        if (data.length == 0) {
            client.sendMessage("&cInvalid LockLogin resource manager command; Expecting <argument> but got nothing");
            client.sendMessage("&cRun&7 lrm help&c for help");
            return;
        }

        client.sendMessage("&dLockLogin Resource Manager");
        client.sendMessage("");

        if (runningLRM) {
            client.sendMessage("&cLockLogin Resource Manager is already running by other agent, please wait for completion");
            return;
        }

        runningLRM = true;
        MarketPlace market = spigot.getMarketPlace();
        ResourceManager manager = market.getManager();

        String argument = data[1].toLowerCase();
        switch (argument) {
            case "help":
            case "-h":
                client.sendMessage("&7lrm help    [-h]  &b-&a Shows help about lrm");
                client.sendMessage("&7lrm list    [-ls] &b-&a List all the resources");
                client.sendMessage("&7lrm enable  [-e]  &b-&a Enables a lrm resource");
                client.sendMessage("&7lrm disable [-d]  &b-&a Disables a lrm resource");
                client.sendMessage("&7lrm remove  [-r]  &b-&a Removes a lrm resource");
                client.sendMessage("&7lrm market  [-m]  &b-&a Opens the lrm marketplace UI");
                client.sendMessage("&7lrm info    [-i]  &b-&a Shows information about a lrm resource");
                client.sendMessage("&7lrm get     [-g]  &b-&a Downloads a lrm resource");
                runningLRM = false;
                break;
            case "list":
            case "-l":
                Category category = Category.ALL;
                int itemsPerPage = 7;
                int page = 0;
                if (data.length == 3) {
                    category = parseCategory(data[2], category, client);
                }
                if (data.length == 4) {
                    category = parseCategory(data[2], category, client);
                    itemsPerPage = parseNumber(data[3], itemsPerPage, client);
                }
                if (data.length >= 5) {
                    category = parseCategory(data[2], category, client);
                    itemsPerPage = parseNumber(data[3], itemsPerPage, client);
                    page = parseNumber(data[4], page, client);
                }

                Collection<? extends StoredResource> resources = manager.getResources(category, itemsPerPage, page)
                        .stream().sorted(ResourceManager.BY_INSTALLATION_DATE)
                        .collect(Collectors.toList());

                if (resources.isEmpty()) {
                    if (category.equals(Category.EMAIL_TEMPLATE)) {
                        client.sendMessage("&cYou don't have any resource [" + category.prettyName() + "] installed");
                        runningLRM = false;
                        return;
                    }

                    final String categoryName = category.prettyName();
                    market.getPages(category).whenComplete((pages) -> {
                        client.sendMessage("&cYou don't have any resource [" + categoryName + "] installed (" + pages + " resource pages available for " + categoryName + ")");
                        runningLRM = false;
                    });
                    return;
                }

                client.sendMessage("&aListing all downloaded resources");
                for (StoredResource resource : resources) {
                    client.sendMessage(prettyResource(resource));
                }

                client.sendMessage("");
                client.sendMessage("&ePage:&7 " + page + " &8&l|&e Items per page:&7 " + itemsPerPage + " &8&l|&e Category:&7 " + category.prettyName());
                client.sendMessage("&ePages:&7 " + Math.min(manager.getResourceCount(), itemsPerPage / manager.getResourceCount()));
                runningLRM = false;
                break;
            case "enable":
            case "-e":
                if (data.length != 3) {
                    client.sendMessage("&cMissing argument (resource-id)");
                    runningLRM = false;
                    return;
                }

                String rawEnableId = data[2];
                try {
                    int resourceId = Integer.parseInt(rawEnableId);
                    StoredResource resource = market.getManager().getResources().stream()
                            .filter((rs) -> rs.getId() == resourceId).findAny().orElse(null);

                    if (resource == null) {
                        client.sendMessage("&cNo resource with id &7" + resourceId + "&c has been installed");
                        runningLRM = false;
                        return;
                    }

                    if (resource.isLoaded()) {
                        client.sendMessage("&cResource with id &7" + resourceId + "&c is already loaded");
                        runningLRM = false;
                        return;
                    }

                    resource.load();
                    if (resource.isLoaded()) {
                        client.sendMessage("&aSuccessfully enabled resource with id&7 " + resourceId);

                        spigot.configuration().reload(); //Perform a silent reload in order to apply changes from resources
                        spigot.languagePackManager().setLang(spigot.configuration().language());

                        spigot.messages().reload();
                    }

                    runningLRM = false;
                } catch (NumberFormatException ex) {
                    client.sendMessage("&cInvalid resource-id (" + rawEnableId + "). Must be a valid number");
                    runningLRM = false;
                }
                break;
            case "disable":
            case "-d":
                if (data.length != 3) {
                    client.sendMessage("&cMissing argument (resource-id)");
                    runningLRM = false;
                    return;
                }

                String rawDisableId = data[2];
                try {
                    int resourceId = Integer.parseInt(rawDisableId);
                    StoredResource resource = market.getManager().getResources().stream()
                            .filter((rs) -> rs.getId() == resourceId).findAny().orElse(null);

                    if (resource == null) {
                        client.sendMessage("&cNo resource with id &7" + resourceId + "&c has been installed");
                        runningLRM = false;
                        return;
                    }

                    if (!resource.isLoaded()) {
                        client.sendMessage("&cResource with id &7" + resourceId + "&c is not loaded");
                        runningLRM = false;
                        return;
                    }

                    resource.unload();
                    if (!resource.isLoaded()) {
                        client.sendMessage("&aSuccessfully disabled resource with id&7 " + resourceId);

                        spigot.configuration().reload(); //Perform a silent reload in order to apply changes from resources
                        spigot.languagePackManager().setLang(spigot.configuration().language());

                        spigot.messages().reload();
                    }

                    runningLRM = false;
                } catch (NumberFormatException ex) {
                    client.sendMessage("&cInvalid resource-id (" + rawDisableId + "). Must be a valid number");
                    runningLRM = false;
                }
                break;
            case "remove":
            case "-r":
                if (data.length != 3) {
                    client.sendMessage("&cMissing argument (resource-id)");
                    runningLRM = false;
                    return;
                }

                String rawRemoveId = data[2];
                try {
                    int resourceId = Integer.parseInt(rawRemoveId);
                    StoredResource resource = market.getManager().getResources().stream()
                            .filter((rs) -> rs.getId() == resourceId).findAny().orElse(null);

                    if (resource == null) {
                        client.sendMessage("&cNo resource with id &7" + resourceId + "&c has been installed");
                        runningLRM = false;
                        return;
                    }

                    market.getManager().uninstall(resource.getId());
                    client.sendMessage("&aSuccessfully removed resource with id&7 " + resourceId);
                    runningLRM = false;
                } catch (NumberFormatException ex) {
                    client.sendMessage("&cInvalid resource-id (" + rawRemoveId + "). Must be a valid number");
                    runningLRM = false;
                }
                break;
            case "info":
            case "-i":
                if (data.length != 3) {
                    client.sendMessage("&cMissing argument (resource-id)");
                    runningLRM = false;
                    return;
                }

                String rawInfoId = data[2];
                try {
                    int resourceId = Integer.parseInt(rawInfoId);

                    market.getResource(resourceId).whenComplete((resource) -> {
                        if (resource == null) {
                            client.sendMessage("&cCouldn't find resource with id: " + resourceId);
                            runningLRM = false;
                            return;
                        }

                        boolean installed = market.getManager().getResources().stream().anyMatch((rs) ->
                                rs.getId() == resource.getId());
                        boolean needsUpdate = market.getManager().getResources().stream().noneMatch((rs) -> rs.getId() == resource.getId() &&
                                rs.getVersion().equals(resource.getVersion()));

                        client.sendMessage((installed ? "&a[INSTALLED] " : "&c[NOT INSTALLED] ") + "&d" + resource.getName());
                        client.sendMessage("&7Resource id: &b" + resource.getId());
                        client.sendMessage("&7Category: &b" + resource.getCategory());
                        client.sendMessage("&7Author: &b" + resource.getPublisher());
                        client.sendMessage("&7Version: &b" + resource.getVersion() + (needsUpdate && installed ? "&e (update available)" : (installed ? "&a (up-to-date)" : "")));
                        client.sendMessage("&7Information: &b" + resource.getDescription());
                        client.sendMessage("&7Downloads: &b" + resource.getDownloads());
                        if (installed) {
                            if (needsUpdate) {
                                client.sendMessage("");
                                client.sendMessage("&7Run lrm get &b" + resource.getId() + "&7 to update the resource");
                            }
                        } else {
                            client.sendMessage("");
                            client.sendMessage("&7Run lrm get &b" + resource.getId() + "&7 to install the resource");
                        }

                        runningLRM = false;
                    });
                } catch (NumberFormatException ex) {
                    client.sendMessage("&cInvalid resource-id (" + rawInfoId + "). Must be a valid number");
                    runningLRM = false;
                }
                break;
            case "market":
            case "-m":
                AtomicReference<Category> marketCategory = new AtomicReference<>(Category.ALL);
                AtomicInteger marketPage = new AtomicInteger(1);
                if (data.length == 3) {
                    Category requiredCategory = parseCategory(data[2], marketCategory.get(), client);
                    marketCategory.set(requiredCategory);
                }
                if (data.length == 4) {
                    Category requiredCategory = parseCategory(data[2], marketCategory.get(), client);
                    marketCategory.set(requiredCategory);

                    int requiredPage = parseNumber(data[3], marketPage.get(), client);
                    marketPage.set(Math.max(1, requiredPage));
                }

                client.sendMessage("&aListing all the resources for " + marketCategory.get().prettyName() + " on page " + marketPage);
                market.getPages(marketCategory.get()).whenComplete((maxPages) -> {
                    if (maxPages < marketPage.get()) {
                        marketPage.set(maxPages);
                        client.sendMessage("&e[WARNING]&7 Max resource category for " + marketCategory.get().prettyName() + " page reached");
                    }

                    market.getResources(marketCategory.get(), marketPage.get()).whenComplete((rsList) -> {
                        if (rsList == null || rsList.isEmpty()) {
                            client.sendMessage("&cNo resources found");
                            runningLRM = false;
                            return;
                        }

                        for (MarketResource resource : rsList) {
                            boolean installed = market.getManager().getResources().stream().anyMatch((rs) ->
                                    rs.getId() == resource.getId());
                            boolean needsUpdate = market.getManager().getResources().stream().noneMatch((rs) -> rs.getId() == resource.getId() &&
                                    rs.getVersion().equals(resource.getVersion()));

                            client.sendMessage((installed ? "&a[INSTALLED] " : "") + "&d" + resource.getName());
                            client.sendMessage("&7Resource id: &b" + resource.getId());
                            client.sendMessage("&7Category: &b" + resource.getCategory());
                            client.sendMessage("&7Author: &b" + resource.getPublisher());
                            client.sendMessage("&7Version: &b" + resource.getVersion() + (needsUpdate && installed ? "&e (update available)" : (installed ? "&a (up-to-date)" : "")));
                            client.sendMessage("&7Information: &b" + resource.getDescription());
                            client.sendMessage("&7Downloads: &b" + resource.getDownloads());
                            if (installed) {
                                if (needsUpdate) {
                                    client.sendMessage("");
                                    client.sendMessage("&7Run lrm get &b" + resource.getId() + "&7 to update the resource");
                                }
                            } else {
                                client.sendMessage("");
                                client.sendMessage("&7Run lrm get &b" + resource.getId() + "&7 to install the resource");
                            }
                            client.sendMessage("&d------------------------------");
                        }

                        client.sendMessage("&ePage: &7" + marketPage.get() + " &8&l| &eMax pages: &7" + maxPages + " &8&l| &eResources: &7" + rsList.size());
                        runningLRM = false;
                    });
                });
                break;
            case "get":
            case "-g":
                if (data.length != 3) {
                    client.sendMessage("&cMissing argument (resource-id)");
                    runningLRM = false;
                    return;
                }

                String rawId = data[2];
                try {
                    int resourceId = Integer.parseInt(rawId);
                    MarketResource existing = downloadQue.get(client);
                    if (existing != null && existing.getId() == resourceId) {
                        client.sendMessage("&aProceeding with download of resource with id&7 " + resourceId + "&a (&7" + existing.getName() + "&a) version&7 " + existing.getVersion());
                        downloadQue.remove(client);

                        ResourceDownload download = existing.getDownload();
                        download.download().whenComplete((success) -> {
                            StoredResource sr = manager.getResources().stream().filter((rs) -> rs.getId() == resourceId).findAny().orElse(null);
                            if (sr == null) {
                                client.sendMessage("&cFailed to download resource with id&7 " + resourceId + "&c. Probably it is not download-able");
                                runningLRM = false;
                                return;
                            }
                            if (success == null || !success) {
                                client.sendMessage("&eSuccessfully downloaded resource with id&7 " + resourceId + "&e but failed to enable.&c Is it compatible?");
                                runningLRM = false;
                                return;
                            }

                            client.sendMessage("&aSuccessfully downloaded resource with id&7 " + resourceId);
                            runningLRM = false;
                        });

                        return;
                    }

                    market.getResource(resourceId).whenComplete((resource) -> {
                        if (resource == null) {
                            client.sendMessage("&cCouldn't find resource with id: " + resourceId);
                            runningLRM = false;
                            return;
                        }

                        client.sendMessage("&aFound resource with id&7 " + resourceId + "&a named&b " + resource.getName());
                        client.sendMessage("");
                        client.sendMessage("&aResource author: &7" + resource.getPublisher());
                        client.sendMessage("&aResource category: &7" + resource.getCategory());
                        client.sendMessage("&aResource version: &7" + resource.getVersion());
                        client.sendMessage("&aResource downloads: &7" + resource.getDownloads());
                        client.sendMessage("&aRun the command again in order to proceed with download");
                        downloadQue.put(client, resource);
                        runningLRM = false;
                    });
                } catch (NumberFormatException ex) {
                    client.sendMessage("&cInvalid resource-id (" + rawId + "). Must be a valid number");
                    runningLRM = false;
                }
                break;
            default:
                client.sendMessage("&c\"" + argument + "\" is not a known function");
                runningLRM = false;
                break;
        }
    }

    private static Category parseCategory(final String value, final Category currentValue, final TextContainer client) {
        try {
            return Category.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            client.sendMessage("&e[WARNING]&7 Unknown category: " + value);
        }

        return currentValue;
    }

    private static int parseNumber(final String value, final int currentValue, final TextContainer client) {
        try {
            return Integer.parseInt(value);
        } catch (IllegalArgumentException ex) {
            client.sendMessage("&e[WARNING]&7 Invalid number: " + value);
        }

        return currentValue;
    }

    private static String prettyResource(final StoredResource resource) {
        String name = resource.getName();
        String author = resource.getPublisher();
        String category = resource.getCategory().name().toLowerCase();
        String version = resource.getVersion();

        return String.format("&8[&d%s&8] &7%s&f v&7%s &7By&e %s&7 (resource id: &d%d&7)", category, name, version, author, resource.getId());
    }
}
