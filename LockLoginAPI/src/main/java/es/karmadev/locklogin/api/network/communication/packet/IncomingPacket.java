package es.karmadev.locklogin.api.network.communication.packet;

import com.google.gson.JsonArray;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import org.jetbrains.annotations.Nullable;

/**
 * LockLogin incoming packet
 */
public interface IncomingPacket extends ComPacket {

    /**
     * Get the message data type
     *
     * @return the message type
     */
    DataType getType();

    /**
     * Get a character sequence of the
     * packet
     *
     * @param key the key
     * @return the string
     */
    @Nullable
    String getSequence(final String key);

    /**
     * Get a number of the packet
     *
     * @param key the key
     * @return the number
     */
    @Nullable
    Number getNumber(final String key);

    /**
     * Get a character of the packet
     *
     * @param key the key
     * @return the character
     */
    char getCharacter(final String key);

    /**
     * Get an integer of the packet
     *
     * @param key the key
     * @return the integer
     */
    int getInteger(final String key);

    /**
     * Get a long of the packet
     *
     * @param key the key
     * @return the long
     */
    long getLong(final String key);

    /**
     * Get a double of the packet
     *
     * @param key the key
     * @return the double
     */
    double getDouble(final String key);

    /**
     * Get a float of the packet
     *
     * @param key the key
     * @return the float
     */
    float getFloat(final String key);

    /**
     * Get a short of the packet
     *
     * @param key the key
     * @return the short
     */
    short getShort(final String key);

    /**
     * Get a byte of the packet
     *
     * @param key the key
     * @return the byte
     */
    byte getByte(final String key);

    /**
     * Get a packet part of the packet
     *
     * @param key the key
     * @return the packet part
     * @throws InvalidPacketDataException if the packet is not correctly
     * defined in the packet message
     */
    @Nullable
    IncomingPacket getObject(final String key) throws InvalidPacketDataException;

    /**
     * Get a list of the packet
     *
     * @param key the key
     * @return the list
     */
    @Nullable
    JsonArray getList(final String key);
}
