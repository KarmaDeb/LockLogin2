package es.karmadev.locklogin.common.api.packet;

import com.google.gson.*;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class CInPacket implements IncomingPacket {

    private final byte[] data;
    private transient JsonObject json;
    private final int id;
    private final DataType type;
    private final Instant stamp;

    /**
     * Create the packet
     *
     * @param raw the raw packet data
     * @throws InvalidPacketDataException if the raw packet data is not valid
     */
    public CInPacket(final String raw) throws InvalidPacketDataException {
        this.data = raw.getBytes();

        try {
            Gson gson = new GsonBuilder().create();
            JsonElement element = gson.fromJson(raw, JsonElement.class);
            if (!element.isJsonObject()) {
                throw new InvalidPacketDataException("Cannot parse packet data because is not a JsonObject");
            }

            json = element.getAsJsonObject();
            if (!json.has("id")) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet ID is missing");
            }
            if (!json.has("packetType")) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet type is missing");
            }
            if (!json.has("stamp")) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet stamp is missing");
            }

            JsonElement idElement = json.get("id");
            JsonElement typeElement = json.get("packetType");
            JsonElement stampElement = json.get("stamp");

            if (!idElement.isJsonPrimitive()) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet ID has invalid format");
            }
            if (!typeElement.isJsonPrimitive()) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet type has invalid format");
            }
            if (!stampElement.isJsonPrimitive()) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet stamp has invalid format");
            }

            JsonPrimitive idPrimitive = idElement.getAsJsonPrimitive();
            JsonPrimitive typePrimitive = typeElement.getAsJsonPrimitive();
            JsonPrimitive stampPrimitive = stampElement.getAsJsonPrimitive();

            if (!idPrimitive.isNumber()) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet ID is not expected type");
            }
            if (!typePrimitive.isString()) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet type is not expected type");
            }
            if (!stampPrimitive.isNumber()) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet stamp is not expected type");
            }

            id = idPrimitive.getAsNumber().intValue();
            String rawType = typePrimitive.getAsString();
            try {
                type = DataType.valueOf(rawType);
            } catch (IllegalArgumentException ex) {
                throw new InvalidPacketDataException("Cannot parse packet data because packet type is unknown type");
            }

            long stampMillis = stampPrimitive.getAsLong();
            stamp = Instant.ofEpochMilli(stampMillis);
            Instant now = Instant.now();

            Duration duration = Duration.between(stamp, now);
            if (duration.getSeconds() >= 10) {
                throw new InvalidPacketDataException("Cannot parse packet data because it expired");
            }
        } catch (JsonSyntaxException ex) {
            throw new InvalidPacketDataException(ex);
        }
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
     * Get the packet data
     *
     * @return the packet data
     */
    @Override
    public byte[] getData() {
        return data.clone();
    }

    /**
     * Get the message data type
     *
     * @return the message type
     */
    @Override
    public DataType getType() {
        return type;
    }

    /**
     * Get a character sequence of the
     * packet
     *
     * @param key the key
     * @return the string
     */
    @Override @Nullable
    public String getSequence(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) return element.getAsString();
        }

        return null;
    }

    /**
     * Get a number of the packet
     *
     * @param key the key
     * @return the number
     */
    @Override @Nullable
    public Number getNumber(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) return element.getAsNumber();
        }

        return null;
    }

    /**
     * Get a character of the packet
     *
     * @param key the key
     * @return the character
     */
    @Override
    public char getCharacter(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) return primitive.getAsCharacter();
            if (primitive.isNumber()) return (char) primitive.getAsNumber().intValue();
        }

        return 0;
    }

    /**
     * Get an integer of the packet
     *
     * @param key the key
     * @return the integer
     */
    @Override
    public int getInteger(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) return element.getAsInt();
        }

        return -1;
    }

    /**
     * Get a long of the packet
     *
     * @param key the key
     * @return the long
     */
    @Override
    public long getLong(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) return primitive.getAsLong();
        }

        return -1;
    }

    /**
     * Get a double of the packet
     *
     * @param key the key
     * @return the double
     */
    @Override
    public double getDouble(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) return element.getAsDouble();
        }

        return -1;
    }

    /**
     * Get a float of the packet
     *
     * @param key the key
     * @return the float
     */
    @Override
    public float getFloat(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) return element.getAsFloat();
        }

        return -1;
    }

    /**
     * Get a short of the packet
     *
     * @param key the key
     * @return the short
     */
    @Override
    public short getShort(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) return element.getAsShort();
        }

        return -1;
    }

    /**
     * Get a byte of the packet
     *
     * @param key the key
     * @return the byte
     */
    @Override
    public byte getByte(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) return element.getAsByte();
        }

        return -1;
    }

    /**
     * Get a packet part of the packet
     *
     * @param key the key
     * @return the packet part
     * @throws InvalidPacketDataException if the packet is not correctly
     * defined in the packet message
     */
    @Override @Nullable
    public IncomingPacket getObject(final String key) throws InvalidPacketDataException {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            String raw = gson.toJson(object);

            return new CInPacket(raw);
        }

        return null;
    }

    /**
     * Get a list of the packet
     *
     * @param key the key
     * @return the list
     */
    @Override @Nullable
    public JsonArray getList(final String key) {
        ensureJson();
        JsonElement element = find(key);
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }

        return null;
    }

    /**
     * Find an element by key
     *
     * @param key the element key
     * @return the element
     * @throws IllegalStateException if the key is invalid
     */
    private JsonElement find(final String key) throws IllegalStateException {
        ensureJson();
        if (json.has(key)) {
            return json.get(key);
        }

        if (key.contains(".")) {
            String[] keys = key.split("\\.");
            JsonObject element = json;
            int index = 0;
            for (String point : keys) {
                if (!element.has(point)) {
                    throw new IllegalStateException("Cannot get json key " + key + " because " + point + " is not set");
                }

                JsonElement sub = element.get(point);
                if (!sub.isJsonObject() && index != (keys.length - 1)) {
                    throw new IllegalStateException("Cannot get json key " + key + " because " + point + " does not point to a section");
                }

                element = sub.getAsJsonObject();
                index++;
            }

            return element;
        }

        throw new IllegalStateException("Cannot get json key " + key + " because is not defined");
    }

    /**
     * Ensure json object
     */
    private void ensureJson() {
        if (json == null) {
            Gson gson = new GsonBuilder().create();
            json = gson.fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);
        }
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return json.toString();
    }
}
