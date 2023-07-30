package es.karmadev.locklogin.spigot.command.module;

import lombok.Getter;
import lombok.Value;
import org.bukkit.command.CommandSender;

/**
 * LockLogin plugin command
 */
@Getter
@Value(staticConstructor = "of")
public class PluginCommand {

    CommandSender sender;

    String label;

    String[] args;
}
