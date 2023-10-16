package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.mail.EmailService;
import es.karmadev.locklogin.common.api.plugin.service.mail.CMailMessage;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@PluginCommand(command = "testmail")
public class TestMail extends Command {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();

    public TestMail(final String cmd) {
        super(cmd);
    }

    /**
     * Executes the command, returning its success
     *
     * @param sender       Source object which is executing this command
     * @param label        The alias of the command used
     * @param args         All arguments passed to the command, split via ' '
     * @return true if the command was successful, otherwise false
     */
    @Override
    public boolean execute(final @NotNull CommandSender sender, final @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(ColorComponent.parse("&5&oThis command is exclusive for console"));
            return false;
        }

        if (args.length >= 3) {
            String target = args[0];
            String subject = args[1];

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                messageBuilder.append(args[i]);
                if (i < args.length - 1) {
                    messageBuilder.append(" ");
                }
            }

            String message = messageBuilder.toString();

            PluginService service = plugin.getService("mailer");
            if (service instanceof EmailService) {
                EmailService mailer = (EmailService) service;
                Configuration configuration = plugin.configuration();

                if (!configuration.mailer().isEnabled()) {
                    sender.sendMessage(ColorComponent.parse("&5&oMailer service is not enabled, please configure it"));
                    return false;
                }

                sender.sendMessage(ColorComponent.parse("&dTrying to send test email to " + target + " as noreply@karmadev.es"));
                mailer.send(target, CMailMessage.builder().origin("noreply@karmadev.es").subject(subject).message(message).build());
                return false;
            }

            sender.sendMessage(ColorComponent.parse("&5&oFailed to fetch mailer service"));
            return false;
        }

        sender.sendMessage(ColorComponent.parse("&5&oPlease, specify a target, a subject and the mail message"));
        return false;
    }
}
