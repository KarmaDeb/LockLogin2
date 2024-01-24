package es.karmadev.locklogin.api.network.communication.packet.listener.event;

import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import lombok.Getter;

/**
 * Represents a network event
 */
@Getter
public abstract class NetworkEvent {

    private final NetworkChannel channel;

    public NetworkEvent(final NetworkChannel channel) {
        this.channel = channel;
    }
}
