package es.karmadev.locklogin.common.api.packet;

import es.karmadev.api.kson.JsonArray;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.kson.io.JsonReader;
import es.karmadev.api.kson.object.JsonNull;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class COutPacket implements OutgoingPacket {

    //private final static LockLogin plugin = CurrentPlugin.getPlugin();
    private transient JsonObject json = JsonObject.newObject("", "");
    private final Map<Integer, String> byteMap = new ConcurrentHashMap<>();
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

        json.put("id", id);
        json.put("packetType", type.name());
        json.put("stamp", stamp.toEpochMilli());
        updateData();
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
        ensureJson();
        json.put(key, String.valueOf(character));

        updateData();
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
        ensureJson();
        if (value == null) {
            json.put(key, JsonNull.get());
            return this;
        }

        json.put(key, value.toString());
        updateData();
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
        ensureJson();
        json.put(key, value);

        updateData();
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
        ensureJson();
        if (number == null) {
            json.put(key, JsonNull.get());
            return this;
        }

        json.put(key, number);

        updateData();
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
        ensureJson();

        if (array == null) {
            json.put(key, JsonNull.get());
            return this;
        }

        json.put(key, array);

        updateData();
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
        ensureJson();

        if (packet == null) {
            json.put(key, JsonNull.get());
            return this;
        }

        JsonObject toAppend = packet.build();
        if (toAppend == null) {
            json.put(key, JsonNull.get());
            return this;
        }

        DataType fromType = packet.getType();
        if (!fromType.equals(type)) {
            toAppend.put("id", id);
            toAppend.put("packetType", fromType.name());
            toAppend.put("stamp", packet.timestamp().toEpochMilli());
        }

        json.put(key, toAppend);

        updateData();
        return this;
    }

    /**
     * Build the packet
     *
     * @return the packet data
     */
    @Override
    public JsonObject build() {
        ensureJson();

        json.put("id", id);
        json.put("packetType", type.name());
        json.put("stamp", stamp.toEpochMilli());

        updateData();
        return json;
    }

    private void updateData() {
        if (json == null) return;

        byteMap.clear();
        byte[] outData = json.toString(false).getBytes();

        for (int i = 0; i < outData.length; i++) {
            String encoded = Byte.toString(outData[i]);
            byteMap.put(i, encoded);

            //plugin.info("[OutPacket] Encoded {0} to {1}({2})", i, encoded, outData[i]);
        }
    }

    private void ensureJson() {
        if (json == null) {
            byte[] byteCreator = new byte[byteMap.size()];
            for (int i = 0; i < byteMap.size(); i++) {
                String encoded = byteMap.get(i);
                byte decoded = Byte.parseByte(encoded);

                byteCreator[i] = decoded;
                //plugin.info("[OutPacket] Decoded {0} to {1}({2})", i, encoded, decoded);
            }

            json = JsonReader.parse(byteCreator).asObject();
        }
    }
}
