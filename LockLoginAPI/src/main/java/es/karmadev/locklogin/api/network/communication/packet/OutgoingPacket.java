package es.karmadev.locklogin.api.network.communication.packet;

import es.karmadev.api.kson.JsonArray;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.locklogin.api.network.communication.data.DataType;

/**
 * LockLogin outgoing packet
 */
public interface OutgoingPacket extends ComPacket {

    /**
     * Get the packet data type
     *
     * @return the data type
     */
    DataType getType();

    /**
     * Add a single character to the
     * packet data
     *
     * @param key the key
     * @param character the character
     * @throws UnsupportedOperationException implementations may
     * throw this
     * @return the outgoing packet
     */
    OutgoingPacket addProperty(final String key, final char character) throws UnsupportedOperationException;

    /**
     * Add a sequence of characters to
     * the packet data
     *
     * @param key the key
     * @param value the characters
     * @throws UnsupportedOperationException implementations may
     * throw this
     * @return the outgoing packet
     */
    OutgoingPacket addProperty(final String key, final CharSequence value) throws UnsupportedOperationException;

    /**
     * Add a boolean to the packet data
     *
     * @param key the data key
     * @param value the boolean value
     * @throws UnsupportedOperationException implementations may
     * throw this
     * @return the outgoing packet
     */
    OutgoingPacket addProperty(final String key, final boolean value) throws UnsupportedOperationException;

    /**
     * Add a number to the packet data
     *
     * @param key the data key
     * @param number the number to add
     * @throws UnsupportedOperationException implementations may
     * throw this
     * @return the outgoing packet
     */
    OutgoingPacket addProperty(final String key, final Number number) throws UnsupportedOperationException;

    /**
     * Add a list to the packet data
     *
     * @param key the data key
     * @param array the array to add
     * @throws UnsupportedOperationException implementations may
     * throw this
     * @return the outgoing packet
     */
    OutgoingPacket addList(final String key, final JsonArray array) throws UnsupportedOperationException;

    /**
     * Append a packet to this one
     *
     * @param key the packet key
     * @param packet the packet
     * @throws UnsupportedOperationException implementations may
     * throw this
     * @return the outgoing packet
     */
    OutgoingPacket append(final String key, final OutgoingPacket packet) throws UnsupportedOperationException;

    /**
     * Build the packet
     *
     * @return the packet data
     * @throws UnsupportedOperationException implementations may
     * throw this
     */
    JsonObject build() throws UnsupportedOperationException;
}
