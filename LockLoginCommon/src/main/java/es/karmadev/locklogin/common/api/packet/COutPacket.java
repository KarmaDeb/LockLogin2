package es.karmadev.locklogin.common.api.packet;

import com.google.gson.*;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class COutPacket implements OutgoingPacket {

    //private final static LockLogin plugin = CurrentPlugin.getPlugin();
    private transient JsonObject json = new JsonObject();
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

        json.addProperty("id", id);
        json.addProperty("packetType", type.name());
        json.addProperty("stamp", stamp.toEpochMilli());
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
        json.addProperty(key, character);

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
            json.add(key, JsonNull.INSTANCE);
            return this;
        }

        json.addProperty(key, value.toString());
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
        json.addProperty(key, value);

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
            json.add(key, JsonNull.INSTANCE);
            return this;
        }

        json.addProperty(key, number);

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
            json.add(key, JsonNull.INSTANCE);
            return this;
        }

        json.add(key, array);

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

        json.addProperty("id", id);
        json.addProperty("packetType", type.name());
        json.addProperty("stamp", stamp.toEpochMilli());

        updateData();
        return json.deepCopy();
    }

    private void updateData() {
        if (json == null) return;

        byteMap.clear();
        Gson gson = new GsonBuilder().create();
        byte[] outData = gson.toJson(json).getBytes(StandardCharsets.UTF_8);

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

            Gson gson = new GsonBuilder().create();
            json = gson.fromJson(new String(byteCreator, StandardCharsets.UTF_8), JsonObject.class);
        }
    }
}
