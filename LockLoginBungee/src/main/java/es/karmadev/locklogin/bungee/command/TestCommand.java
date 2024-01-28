package es.karmadev.locklogin.bungee.command;

import es.karmadev.api.strings.StringOptions;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.protocol.LockLoginProtocol;
import es.karmadev.locklogin.bungee.LockLoginBungee;
import es.karmadev.locklogin.common.api.packet.COutPacket;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Command;

public class TestCommand extends Command {

    private final static LockLoginBungee plugin = (LockLoginBungee) CurrentPlugin.getPlugin();

    public TestCommand() {
        super("test");
    }

    @Override
    public void execute(final CommandSender commandSender, final String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&cFor players only!")));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        Server server = player.getServer();
        if (server == null) return;

        ServerInfo info = server.getInfo();
        String name = info.getName();

        LockLoginProtocol protocol = plugin.getProtocol();

        OutgoingPacket packet = new COutPacket(DataType.HELLO);
        protocol.write(name, String.format("%s_%s",
                StringUtils.generateString(4, StringOptions.LOWERCASE),
                StringUtils.generateString(6, StringOptions.LOWERCASE)),
                packet);
    }
}
