package es.karmadev.locklogin.spigot.nms.v1_15_R1;

import es.karmadev.locklogin.spigot.api.NMSPayload;
import es.karmadev.locklogin.spigot.api.PayloadPacket;

public class CustomPayloadMaker extends NMSPayload {

    {
        registerPayload();
    }

    /**
     * Create a packet
     *
     * @param namespace the packet namespace
     * @param data      the packet data
     * @return the packet
     */
    @Override
    public PayloadPacket createPacket(final String namespace, final byte[] data) {
        return new CustomPayload(namespace, data);
    }
}
