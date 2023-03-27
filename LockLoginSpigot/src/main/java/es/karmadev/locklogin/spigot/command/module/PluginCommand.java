package es.karmadev.locklogin.spigot.command.module;

import lombok.Getter;
import lombok.Value;
import org.bukkit.command.CommandSender;

/**
 * LockLogin plugin command
 */
@Value(staticConstructor = "of")
public class PluginCommand {

    @Getter
    CommandSender sender;

    @Getter
    String label;

    @Getter
    String[] args;
}
