package es.karmadev.locklogin.bungee.command;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.bungee.LockLoginBungee;
import es.karmadev.locklogin.bungee.packet.PacketDataHandler;
import es.karmadev.locklogin.common.api.packet.COutPacket;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Command;

import java.util.Base64;

public class TestCommand extends Command {

    private final static LockLoginBungee plugin = (LockLoginBungee) CurrentPlugin.getPlugin();

    public TestCommand() {
        super("test");
    }

    @Override
    public void execute(final CommandSender commandSender, final String[] strings) {
        OutgoingPacket outgoing = new COutPacket(DataType.HELLO);
        byte[] encodedKey = plugin.getCommunicationKeys().getPublic().getEncoded();
        outgoing.addProperty("key", Base64.getEncoder().encodeToString(encodedKey));

        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            Server server = player.getServer();

            PacketDataHandler.emitPacket(server, outgoing).then((incoming) -> {
                player.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&aReceived hello!")));

                OutgoingPacket channelPacket = new COutPacket(DataType.CHANNEL_INIT);
                channelPacket.addProperty("key", Base64.getEncoder().encodeToString(plugin.getSharedSecret().getEncoded()));

                PacketDataHandler.emitPacket(server, channelPacket).then((response) -> {
                    OutgoingPacket initPacket = new COutPacket(DataType.CONNECTION_INIT);
                    PacketDataHandler.emitPacket(server, initPacket).then((initResponse) -> {
                        String world = initResponse.getSequence("hello");
                        player.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&aReceived: &c" + world)));
                    });
                });
            });
        } else {
            commandSender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&cFor players only!")));
        }
    }
}
