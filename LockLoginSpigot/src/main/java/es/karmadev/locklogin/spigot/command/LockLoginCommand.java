package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.api.strings.ListSpacer;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.ModuleLoader;
import es.karmadev.locklogin.api.extension.module.ModuleManager;
import es.karmadev.locklogin.api.extension.module.PluginModule;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.common.plugin.InternalMessage;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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

                            String postLanguage = configuration.language();
                            if (!preLanguage.equals(postLanguage)) {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_SUCCESS("locklogin", "reload lang", null)));

                                InternalMessage.update();
                            }

                            if (msgReloaded) {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_SUCCESS("locklogin", "reload messages", null)));
                            } else {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_FAIL("locklogin", "reload messages", null)));
                            }
                            if (cfgReloaded) {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_SUCCESS("locklogin", "reload configuration", null)));
                            } else {
                                sender.sendMessage(ColorComponent.parse(messages.prefix() +
                                        InternalMessage.RESPONSE_FAIL("locklogin", "reload messages", null)));
                            }
                        } else{
                            sender.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_RELOAD));
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
                                            } else {
                                                sender.sendMessage(messages.prefix() + InternalMessage.RESPONSE_FAIL("locklogin", "modules info", modName));
                                            }
                                        }
                                    } else {
                                        sender.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_MODULE_LIST));
                                    }
                                    break;
                                case "list":
                                    if (hasPermission(sender, LockLoginPermission.PERMISSION_MODULE_LIST)) {
                                        listModules(sender);
                                    } else {
                                        sender.sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_MODULE_LIST));
                                    }
                                    break;
                                case "load":
                                    break;
                                case "unload":
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
        for (int i = 0; i < modules.length; i++) {
            Module module = modules[i];
            String color = "&c";
            if (module.isEnabled()) {
                color = "&a";
            }
            if (module instanceof PluginModule) {
                modList.append("&8[&ePlugin&8]").append(color).append(" ").append(module.getDescription().getName());
            } else {
                modList.append(color).append(" ").append(module.getDescription().getName());
            }

            if (i < modules.length - 1) {
                modList.append("&7,");
            }
        }

        sender.sendMessage(ColorComponent.parse("&dModules &8&l(&e" + modules.length + "&8&l)&7: " + modList));
    }
}
