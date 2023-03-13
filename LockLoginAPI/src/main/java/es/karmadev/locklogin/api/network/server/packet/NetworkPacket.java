package es.karmadev.locklogin.api.network.server.packet;

import es.karmadev.locklogin.api.extension.Module;

/**
 * Network packet message
 */
@SuppressWarnings("unused")
public interface NetworkPacket {

    /**
     * Get the packet priority
     *
     * @return the packet priority
     */
    int priority();

    /**
     * Get the module that is trying to send the packet
     *
     * @return the module sending the packet
     */
    Module sender();

    /**
     * Get the message
     *
     * @return the message
     */
    byte[] message();
}
