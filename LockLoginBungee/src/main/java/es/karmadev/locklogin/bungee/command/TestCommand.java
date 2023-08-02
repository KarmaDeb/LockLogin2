package es.karmadev.locklogin.bungee.command;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.PluginMessage;

public class TestCommand extends Command {


    public TestCommand() {
        super("test");
    }

    @Override
    public void execute(final CommandSender commandSender, final String[] strings) {
        DefinedPacket message = new PluginMessage("test:test", new byte[0], false);

        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            Server server = player.getServer();

            server.unsafe().sendPacket(message);

            player.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&aDone!")));
        } else {
            commandSender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&cFor players only!")));
        }
    }
}
