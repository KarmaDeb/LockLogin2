package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.mail.EmailService;
import es.karmadev.locklogin.api.plugin.service.mail.MailMessage;
import es.karmadev.locklogin.common.api.plugin.service.mail.CMailMessage;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

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
            sender.sendMessage(Colorize.colorize("&5&oThis command is exclusive for console"));
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

            Configuration configuration = plugin.configuration();
            String senderName = configuration.mailer().getSendAs();

            String message = messageBuilder
                    .append("<br>\n<br>\nThis was a test email from LockLogin, requested by ")
                    .append(sender.getName())
                    .append("<br>\n<br>\n")
                    .append("LockLogin and its developers does not take responsibility over this email or the contents it might have").toString();
            MailMessage mailMessage = CMailMessage.builder().origin(senderName).subject(subject).message(message).build();

            Path template = plugin.workingDirectory().resolve("templates").resolve("mailer").resolve("forgot_password.html");
            if (!Files.exists(template)) {
                plugin.plugin().export("plugin/html/forgot_password.html", template);
            }

            if (args[2].equalsIgnoreCase("template")) {
                sender.sendMessage(Colorize.colorize("&dSending forgot password template mail"));

                mailMessage = CMailMessage.builder(template.toFile())
                        .origin("noreply@karmadev.es")
                        .subject("Your password recovery code")
                        .applyPlaceholder("player", sender.getName())
                        .applyPlaceholder("code", "TEST")
                        .build();
            }

            PluginService service = plugin.getService("mailer");
            if (service instanceof EmailService) {
                EmailService mailer = (EmailService) service;

                if (!configuration.mailer().isEnabled()) {
                    sender.sendMessage(Colorize.colorize("&5&oMailer service is not enabled, please configure it"));
                    return false;
                }

                sender.sendMessage(Colorize.colorize("&dTrying to send test email to " + target + " as " + senderName));
                mailer.send(target, mailMessage);
                return false;
            }

            sender.sendMessage(Colorize.colorize("&5&oFailed to fetch mailer service"));
            return false;
        }

        sender.sendMessage(Colorize.colorize("&5&oPlease, specify a target, a subject and the mail message"));
        return false;
    }
}
