package es.karmadev.locklogin.spigot.nms.v1_20_R1;

import es.karmadev.locklogin.spigot.api.PayloadPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.PacketPlayOutCustomPayload;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class CustomPayload extends PayloadPacket {

    /**
     * Initialize the payload packet
     *
     * @param namespace the packet namespace
     * @param data      the packet data
     */
    public CustomPayload(final String namespace, final byte[] data) {
        super(namespace, data);
    }

    /**
     * Send the packet
     *
     * @param player the player to send the
     *               packet to
     */
    @Override
    public void send(final Player player) {
        PacketDataSerializer data = new PacketDataSerializer(Unpooled.wrappedBuffer(this.data));
        PacketPlayOutCustomPayload payload = new PacketPlayOutCustomPayload(new MinecraftKey(this.namespace), data);

        CraftPlayer craft = (CraftPlayer) player;
        EntityPlayer human = craft.getHandle();
        human.c.a(payload);
    }
}
