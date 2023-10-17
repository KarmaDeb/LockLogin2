package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.api.strings.ListSpacer;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.api.version.BuildStatus;
import es.karmadev.api.version.Version;
import es.karmadev.api.version.checker.VersionChecker;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.extension.module.*;
import es.karmadev.locklogin.api.extension.module.exception.InvalidDescriptionException;
import es.karmadev.locklogin.api.extension.module.exception.InvalidModuleException;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.common.plugin.InternalMessage;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import es.karmadev.locklogin.spigot.process.SpigotLoginProcess;
import es.karmadev.locklogin.spigot.process.SpigotPinProcess;
import es.karmadev.locklogin.spigot.process.SpigotRegisterProcess;
import es.karmadev.locklogin.spigot.process.SpigotTotpProcess;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@PluginCommand(command = "locklogin", useInBungeecord = true)
public class LockLoginCommand extends Command {

    private final LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();

    public LockLoginCommand(final @NotNull String name) {
        super(name);
    }

    /**
     * Executes the command, returning its success
     *
     * @param sender       Source object which is executing this command
     * @param label        The alias of the command used
     * @param args         All arguments passed to the command, split via ' '
     * @return true if the command was successful, otherwise false
     */
    @Override @SuppressWarnings("unchecked")
    public boolean execute(final @NotNull CommandSender sender, final @NotNull String label, final @NotNull String[] args) {
        sender.sendMessage(InternalMessage.PROCESSING_COMMAND(label));
        spigot.plugin().scheduler("async").schedule(() -> {
            String[] usage = InternalMessage.USAGE("locklogin");
            Messages messages = spigot.messages();
            Configuration configuration = spigot.configuration();

            if (args.length == 0) {
                for (String str : usage) {
                    sender.sendMessage(str);
                }
            } else {
                String argument = args[0].toLowerCase();

                switch (argument) {
                    case "help":
                    default:
                        for (String str : usage) {
                            sender.sendMessage(str);
                        }
                        break;
                    case "reload":
                        if (hasPermission(sender, LockLoginPermission.PERMISSION_RELOAD)) {
                            String preLanguage = configuration.language();

                            boolean cfgReloaded = configuration.reload();
                            boolean msgReloaded = messages.reload();
                            boolean mailReload = configuration.mailer().reload();

                            String postLanguage = configuration.language();
                            if (!preLanguage.equals(postLanguage)) {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_SUCCESS("locklogin", "reload lang", null)));

                                InternalMessage.update();
                            }

                            if (cfgReloaded) {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_SUCCESS("locklogin", "reload configuration", null)));

                                SpigotRegisterProcess.setStatus(configuration.authSettings().register());
                                SpigotLoginProcess.setStatus(configuration.authSettings().login());
                                SpigotPinProcess.setStatus(configuration.authSettings().pin());
                                SpigotTotpProcess.setStatus(configuration.authSettings().totp());

                                try {
                                    spigot.plugin().getCommandHelper().mapCommand(spigot);
                                } catch (IOException | ClassNotFoundException ex) {
                                    throw new RuntimeException(ex);
                                }

                                Bukkit.getServer().getScheduler().runTask(spigot.plugin(), () -> {
                                    for (Player online : Bukkit.getOnlinePlayers()) {
                                        online.updateCommands();
                                    }
                                });

                                PluginManager manager = Bukkit.getPluginManager();

                                //PIN inventory events
                                if (SpigotPinProcess.createDummy().isEnabled()) {
                                    manager.registerEvents(spigot.plugin().getUI_CloseOpenHandler(), spigot.plugin());
                                } else {
                                    InventoryOpenEvent.getHandlerList().unregister(spigot.plugin().getUI_CloseOpenHandler());
                                }

                                spigot.languagePackManager().setLang(configuration.language());
                            } else {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_FAIL("locklogin", "reload configuration", null)));
                            }

                            if (msgReloaded) {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_SUCCESS("locklogin", "reload messages", null)));
                            } else {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_FAIL("locklogin", "reload messages", null)));
                            }
                            if (mailReload) {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_SUCCESS("locklogin", "reload mailer", null)));
                            } else {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_FAIL("locklogin", "reload mailer", null)));
                            }
                        } else{
                            sender.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_RELOAD));
                        }
                        break;
                    case "version":
                        VersionChecker checker = spigot.plugin().getChecker();
                        Version latest = checker.getVersion();
                        Version current = spigot.plugin().sourceVersion();
                        if (latest == null) latest = current;

                        switch (args.length) {
                            case 1:
                                if (hasPermission(sender, LockLoginPermission.PERMISSION_VERSION_VIEW)) {
                                    //Whe just want to get the current version
                                    sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "version latest", latest.toString())));
                                    sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "version current", current.toString())));
                                } else {
                                    sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_VERSION_VIEW)));
                                }
                                break;
                            case 2:
                                String sub = args[1].toLowerCase();
                                switch (sub) {
                                    case "current":
                                        if (hasPermission(sender, LockLoginPermission.PERMISSION_VERSION_VIEW)) {
                                            //Whe just want to get the current version
                                            //sender.sendMessage(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "version latest", latest.toString()));
                                            sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "version current", current.toString())));
                                        } else {
                                            sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_VERSION_VIEW)));
                                        }
                                        break;
                                    case "latest":
                                        if (hasPermission(sender, LockLoginPermission.PERMISSION_VERSION_VIEW)) {
                                            //Whe just want to get the current version
                                            //sender.sendMessage(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "version latest", latest.toString()));
                                            sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "version latest", current.toString())));
                                        } else {
                                            sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_VERSION_VIEW)));
                                        }
                                        break;
                                    case "changelog":
                                        if (hasPermission(sender, LockLoginPermission.PERMISSION_VERSION_CHANGELOG)) {
                                            if (hasPermission(sender, LockLoginPermission.PERMISSION_VERSION_VIEW)) {
                                                sender.sendMessage(ColorComponent.parse("&bVersion: &3{0}&7:", latest));
                                            }

                                            String[] changelog = checker.getChangelog();
                                            for (String line : changelog) sender.sendMessage(ColorComponent.parse(line));
                                        } else {
                                            sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_VERSION_CHANGELOG)));
                                        }
                                        break;
                                    case "history":
                                        if (hasPermission(sender, LockLoginPermission.PERMISSION_VERSION_HISTORY)) {
                                            Version[] history = checker.getVersionHistory();

                                            int behind = 0;
                                            boolean countBehind = false;
                                            for (Version version : history) {
                                                if (countBehind) behind++;
                                                if (!countBehind) {
                                                    if (version.compareTo(current) <= 0) {
                                                        countBehind = true;
                                                    }
                                                }

                                                if (hasPermission(sender, LockLoginPermission.PERMISSION_VERSION_VIEW)) {
                                                    switch (current.compareTo(version)) {
                                                        case 1: //Current version over version
                                                            sender.sendMessage(ColorComponent.parse("&e(UP TO DATE) &bVersion: &7{0}", version));
                                                            break;
                                                        case 0: //Current version is version
                                                            sender.sendMessage(ColorComponent.parse("&a(CURRENT) &bVersion: &7{0}", version));
                                                            break;
                                                        case -1: //Current version is under version
                                                            sender.sendMessage(ColorComponent.parse("&c(OVER YOU) &bVersion: &7{0}", version));
                                                            break;
                                                    }
                                                } else {
                                                    sender.sendMessage(ColorComponent.parse("&bVersion: &7{0}", version));
                                                }
                                            }

                                            if (hasPermission(sender, LockLoginPermission.PERMISSION_VERSION_VIEW)) {
                                                if (behind != 0) {
                                                    sender.sendMessage(ColorComponent.parse("&cYou are (&7{0}&c) versions behind", behind));
                                                } else {
                                                    sender.sendMessage(ColorComponent.parse("&aYou are using the latest version", behind));
                                                }
                                            }
                                        } else {
                                            sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_VERSION_CHANGELOG)));
                                        }
                                        break;
                                    case "check":
                                        sender.sendMessage(ColorComponent.parse(messages.prefix() + "&dChecking for updates..."));
                                        checker.check().onComplete(() -> {
                                            if (checker.getStatus().equals(BuildStatus.OUTDATED)) {
                                                sender.sendMessage(ColorComponent.parse("&cLockLogin has found a new version!"));
                                                sender.sendMessage("");
                                                sender.sendMessage(ColorComponent.parse("&7Current version is:&e {0}", current));
                                                sender.sendMessage(ColorComponent.parse("&7Latest version is:&e {0}", checker.getVersion()));
                                                sender.sendMessage("");

                                                sender.sendMessage(ColorComponent.parse("&7Download latest version from:"));
                                                for (URL url : checker.getUpdateURLs()) {
                                                    sender.sendMessage(ColorComponent.parse("&b- &7{0}", url));
                                                }

                                                sender.sendMessage("");
                                                sender.sendMessage(ColorComponent.parse("&b------ &7Version history&b ------"));
                                                for (Version version : checker.getVersionHistory()) {
                                                    String[] changelog = checker.getChangelog(version);
                                                    if (current.compareTo(version) == 0) {
                                                        sender.sendMessage(ColorComponent.parse("&bVersion: &a(current) &3{0}&7:", version));
                                                    } else {
                                                        sender.sendMessage(ColorComponent.parse("&bVersion: &3{0}&7:", version));
                                                    }

                                                    for (String line : changelog) {
                                                        sender.sendMessage(ColorComponent.parse(line));
                                                    }
                                                }
                                            } else {
                                                sender.sendMessage(ColorComponent.parse("&aLockLogin did not found any update, you are up-to date"));
                                                sender.sendMessage(ColorComponent.parse("&aCurrent version is:&7 {0}", current));
                                                sender.sendMessage(ColorComponent.parse("&aLatest version is:&7 {0}", checker.getVersion()));
                                            }
                                        });
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case 3:
                            default:
                                for (String str : usage) {
                                    sender.sendMessage(str);
                                }
                                break;
                        }

                        break;
                    case "modules":
                        if (args.length >= 2) {
                            ModuleManager manager = spigot.moduleManager();
                            ModuleLoader loader = manager.loader();

                            String subArgument = args[1].toLowerCase();

                            switch (subArgument.toLowerCase()) {
                                case "info":
                                    if (hasPermission(sender, LockLoginPermission.PERMISSION_MODULE_LIST)) {
                                        if (args.length == 2) {
                                            for (Module module : loader.getModules()) {
                                                if (module instanceof PluginModule) {
                                                    PluginModule<JavaPlugin> plugin = (PluginModule<JavaPlugin>) module;

                                                    JavaPlugin javaPlugin = plugin.getPlugin();
                                                    sender.sendMessage(ColorComponent.parse("&7Plugin module: &d{0}", javaPlugin.getName()));
                                                    sender.sendMessage(ColorComponent.parse("&7Version: &d{0}", javaPlugin.getDescription().getVersion()));
                                                    sender.sendMessage(ColorComponent.parse("&7Authors: &d{0}", StringUtils.listToString(javaPlugin.getDescription().getAuthors(), ListSpacer.COMMA)));
                                                    continue;
                                                }

                                                sender.sendMessage(ColorComponent.parse("&7Module: &d{0}", module.getName()));
                                                sender.sendMessage(ColorComponent.parse("&7Version: &d{0}", module.getVersion()));
                                                sender.sendMessage(ColorComponent.parse("&7Author(s): &d{0}", module.getDescription().getAuthor()));
                                            }
                                        } else {
                                            String modName = args[2];
                                            Module module = loader.getModule(modName);

                                            if (module != null) {
                                                if (module instanceof PluginModule) {
                                                    PluginModule<JavaPlugin> plugin = (PluginModule<JavaPlugin>) module;

                                                    JavaPlugin javaPlugin = plugin.getPlugin();
                                                    sender.sendMessage(ColorComponent.parse("&7Plugin module: &d{0}", javaPlugin.getName()));
                                                    sender.sendMessage(ColorComponent.parse("&7Version: &d{0}", javaPlugin.getDescription().getVersion()));
                                                    sender.sendMessage(ColorComponent.parse("&7Authors: &d{0}", StringUtils.listToString(javaPlugin.getDescription().getAuthors(), ListSpacer.COMMA)));
                                                    return;
                                                }

                                                sender.sendMessage(ColorComponent.parse("&7Module: &d{0}", module.getName()));
                                                sender.sendMessage(ColorComponent.parse("&7Version: &d{0}", module.getVersion()));
                                                sender.sendMessage(ColorComponent.parse("&7Author(s): &d{0}", module.getDescription().getAuthor()));
                                                if (module.isFromMarketplace()) {
                                                    sender.sendMessage("");
                                                    sender.sendMessage(ColorComponent.parse("&8(&eThis resource has been installed through the marketplace manager&8)"));
                                                }
                                            } else {
                                                sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_FAIL("locklogin", "modules info", modName)));
                                            }
                                        }
                                    } else {
                                        sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_MODULE_LIST)));
                                    }
                                    break;
                                case "list":
                                    if (hasPermission(sender, LockLoginPermission.PERMISSION_MODULE_LIST)) {
                                        listModules(sender);
                                    } else {
                                        sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_MODULE_LIST)));
                                    }
                                    break;
                                case "load":
                                    if (hasPermission(sender, LockLoginPermission.PERMISSION_MODULE_LOAD)) {
                                        if (args.length >= 3) {
                                            String modName = args[2];
                                            Module module = findModule(modName, loader);

                                            if (module == null || module.isEnabled()) {
                                                sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_FAIL("locklogin", "modules load", modName)));
                                                return;
                                            }

                                            if (loader.enable(module)) {
                                                sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "modules load", modName)));
                                            } else {
                                                sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_FAIL("locklogin", "modules load", modName)));
                                            }
                                        } else {
                                            for (String str : usage) {
                                                sender.sendMessage(str);
                                            }
                                        }
                                    } else {
                                        sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_MODULE_LOAD)));
                                    }
                                    break;
                                case "unload":
                                    if (hasPermission(sender, LockLoginPermission.PERMISSION_MODULE_UNLOAD)) {
                                        if (args.length >= 3) {
                                            String action = "";
                                            String modName = args[2];
                                            Module module = loader.getModule(modName);

                                            if (module == null || !module.isEnabled()) {
                                                sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_FAIL("locklogin", "modules unload", modName)));
                                                return;
                                            }

                                            if (args.length >= 4) {
                                                action = args[3];
                                            }

                                            if (action.equalsIgnoreCase("--hard") || action.equalsIgnoreCase("-h")) {
                                                if (module.isFromMarketplace()) {
                                                    sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_FAIL("locklogin", "modules unload", modName)));
                                                    return;
                                                }

                                                loader.unload(module);
                                            } else {
                                                loader.disable(module);
                                            }

                                            sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "modules unload", modName)));
                                        } else {
                                            for (String str : usage) {
                                                sender.sendMessage(str);
                                            }
                                        }
                                    } else {
                                        sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_MODULE_UNLOAD)));
                                    }
                                    break;
                                case "reload":
                                    if (hasPermission(sender, LockLoginPermission.PERMISSION_MODULE_RELOAD)) {
                                        if (args.length >= 3) {
                                            String modName = args[2];
                                            Module module = loader.getModule(modName);

                                            if (module == null || !module.isEnabled()) {
                                                sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_FAIL("locklogin", "modules reload", modName)));
                                                return;
                                            }

                                            Path file = module.getFile();
                                            loader.unload(module);
                                            try {
                                                Module newModule = loader.load(file);
                                                if (loader.enable(newModule)) {
                                                    sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_SUCCESS("locklogin", "modules reload", modName)));
                                                    return;
                                                }
                                            } catch (InvalidModuleException ignored) {}

                                            sender.sendMessage(ColorComponent.parse(messages.prefix() + InternalMessage.RESPONSE_FAIL("locklogin", "modules reload", modName)));
                                        } else {
                                            for (String str : usage) {
                                                sender.sendMessage(str);
                                            }
                                        }
                                    } else {
                                        sender.sendMessage(ColorComponent.parse(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_MODULE_RELOAD)));
                                    }
                                    break;
                                default:
                                    for (String str : usage) {
                                        sender.sendMessage(str);
                                    }
                            }
                        } else {
                            for (String str : usage) {
                                sender.sendMessage(str);
                            }
                        }

                        break;
                }
            }
        });

        return false;
    }

    private boolean hasPermission(final CommandSender sender, final PermissionObject permission) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            int networkId = UserDataHandler.getNetworkId(player);

            if (networkId > 0) {
                NetworkClient client = spigot.network().getPlayer(networkId);
                return client.hasPermission(permission);
            }
        }

        return true;
    }

    private void listModules(final CommandSender sender) {
        ModuleManager manager = spigot.moduleManager();
        ModuleLoader loader = manager.loader();

        StringBuilder modList = new StringBuilder();
        Module[] modules = loader.getModules().toArray(new Module[0]);
        boolean first = true;
        for (int i = 0; i < modules.length; i++) {
            Module module = modules[i];
            String color = "&c";
            if (module.isEnabled()) {
                color = "&a";
            }
            if (module instanceof PluginModule) {
                if (first) {
                    modList.append(" ");
                }
                modList.append("&8[&ePlugin&8]");
            } else {
                if (module.isFromMarketplace()) {
                    if (first) {
                        modList.append(" ");
                    }

                    modList.append("&8[&eMarketplace&8]");
                }
            }

            first = false;
            modList.append(color).append(" ").append(module.getDescription().getName());

            if (i < modules.length - 1) {
                modList.append("&7,");
            }
        }

        sender.sendMessage(ColorComponent.parse("&dModules &8&l(&e" + modules.length + "&8&l)&7:" + modList));
    }

    private Module findModule(final String name, final ModuleLoader loader) {
        Module module = loader.getModule(name);
        if (module != null) return module;

        Path modulesDirectory = spigot.workingDirectory().resolve("mods");
        /*
        Path alternateModulesDirectory = spigot.workingDirectory().resolve("marketplace").resolve("modules");
        This should be actually handled entirely by the lrm
         */

        return getFromPath(loader, name, modulesDirectory);
    }

    private Module getFromPath(final ModuleLoader loader, final String name, final Path directory) {
        Module module = null;

        if (Files.exists(directory)) {
            try (Stream<Path> files = Files.list(directory)
                    .filter((path) -> PathUtilities.getExtension(path).equals("jar"))) {
                module = loadFromStream(loader, name, files);
            } catch (Exception ignored) {}
        }

        return module;
    }

    private Module loadFromStream(final ModuleLoader loader, final String name, final Stream<Path> files) {
        AtomicReference<Module> reference = new AtomicReference<>();

        files.forEachOrdered((path) -> {
            try {
                ModuleDescription desc = loader.loadDescription(path);
                if (desc.getName().equalsIgnoreCase(name) ||
                        PathUtilities.getName(path).equalsIgnoreCase(name)) {
                    try {
                        Module module = loader.load(path);
                        reference.set(module);
                    } catch (InvalidModuleException ignored) {}
                }
            } catch (InvalidDescriptionException ignored) {}
        });

        return reference.get();
    }
}
