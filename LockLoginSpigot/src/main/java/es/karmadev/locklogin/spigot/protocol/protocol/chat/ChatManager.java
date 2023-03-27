package es.karmadev.locklogin.spigot.protocol.protocol.chat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import org.bukkit.entity.Player;

public class ChatManager {

    public static boolean clearProtocolChat(final Player player) {
        LockLogin plugin = CurrentPlugin.getPlugin();

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.CHAT);
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(""));
        packet.getBytes().write(0, (byte) 2);

        try {
            manager.sendServerPacket(player, packet);
            return true;
        } catch (Throwable error) {
            plugin.log(error, "Failed to clear player chat. Using simple method");
        }

        return false;
    }
}
