package es.karmadev.locklogin.protocol;

import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class QueuedPacket {

    private final String tag;
    private final OutgoingPacket packet;
}
