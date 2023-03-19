package es.karmadev.locklogin.common.plugin.web.communication.packet;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

public class COutPacket implements OutgoingPacket {

    private final JsonObject json = new JsonObject();
    private final DataType type;
    private final int id;
    private final Instant stamp = Instant.now();

    /**
     * Initialize the packet
     */
    public COutPacket(final DataType type) {
        this.type = type;
        SecureRandom tmpRandom;
        try {
            tmpRandom = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException ex) {
            tmpRandom = new SecureRandom();
        }

        id = tmpRandom.nextInt();
    }

    /**
     * Get the packet ID
     *
     * @return the packet id
     */
    @Override
    public int id() {
        return id;
    }

    /**
     * Get the packet timestamp
     *
     * @return the packet timestamp
     */
    @Override
    public Instant timestamp() {
        return stamp;
    }

    /**
     * Get the packet data type
     *
     * @return the data type
     */
    @Override
    public DataType getType() {
        return type;
    }

    /**
     * Add a single character to the
     * packet data
     *
     * @param key the key
     * @param character the character
     * @return the outgoing packet
     */
    @Override
    public OutgoingPacket addProperty(final String key, final char character) {
        json.addProperty(key, character);
        return this;
    }

    /**
     * Add a sequence of characters to
     * the packet data
     *
     * @param key the key
     * @param value the characters
     * @return the outgoing packet
     */
    @Override
    public OutgoingPacket addProperty(final String key, final CharSequence value) {
        if (value == null) {
            json.add(key, JsonNull.INSTANCE);
            return this;
        }

        json.addProperty(key, value.toString());
        return this;
    }

    /**
     * Add a boolean to the packet data
     *
     * @param key the data key
     * @param value the boolean value
     * @return the outgoing packet
     */
    @Override
    public OutgoingPacket addProperty(final String key, final boolean value) {
        json.addProperty(key, value);
        return this;
    }

    /**
     * Add a number to the packet data
     *
     * @param key the data key
     * @param number the number to add
     * @return the outgoing packet
     */
    @Override
    public OutgoingPacket addProperty(final String key, final Number number) {
        if (number == null) {
            json.add(key, JsonNull.INSTANCE);
            return this;
        }

        json.addProperty(key, number);
        return this;
    }

    /**
     * Add a list to the packet data
     *
     * @param key the data key
     * @param array the array to add
     * @return the outgoing packet
     */
    @Override
    public OutgoingPacket addList(final String key, final JsonArray array) {
        if (array == null) {
            json.add(key, JsonNull.INSTANCE);
            return this;
        }

        json.add(key, array);
        return this;
    }

    /**
     * Append a packet to this one
     *
     * @param key the packet key
     * @param packet the packet
     * @return the outgoing packet
     */
    @Override
    public OutgoingPacket append(final String key, final OutgoingPacket packet) {
        if (packet == null) {
            json.add(key, JsonNull.INSTANCE);
            return this;
        }

        JsonObject toAppend = packet.build();
        if (toAppend == null) {
            json.add(key, JsonNull.INSTANCE);
            return this;
        }

        DataType fromType = packet.getType();
        if (!fromType.equals(type)) {
            toAppend.addProperty("id", id);
            toAppend.addProperty("packetType", fromType.name());
            toAppend.addProperty("stamp", packet.timestamp().toEpochMilli());
        }

        json.add(key, toAppend);
        return this;
    }

    /**
     * Build the packet
     *
     * @return the packet data
     */
    @Override
    public JsonObject build() {
        json.addProperty("id", id);
        json.addProperty("packetType", type.name());
        json.addProperty("stamp", stamp.toEpochMilli());
        return json.deepCopy();
    }
}
