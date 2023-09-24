package es.karmadev.locklogin.common.api.packet.frame;

import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CFramePacket implements PacketFrame {

    //private final static LockLogin plugin = CurrentPlugin.getPlugin();
    private final int ownerId;
    private final UUID frameId;
    private final int position;
    private final int frameCount;
    private final Map<Integer, String> byteMap = new ConcurrentHashMap<>();
    private final Instant creation;

    public CFramePacket(final int ownerId, final int position, final int frameCount, final byte[] data, final Instant creation) {
        this.ownerId = ownerId;
        this.frameId = UUID.randomUUID();
        this.position = position;
        this.frameCount = frameCount;
        for (int i = 0; i < data.length; i++) {
            String encoded = Byte.toString(data[i]);
            byteMap.put(i, encoded);

            //plugin.info("[Frame] Encoded {0} to {1}({2})", i, encoded, data[i]);
            /*
            Storing the encoded byte is the only way I've found
            to keep the real byte value when sending the packet
            through BungeeCord or Spigot, as I think minecraft
            encryption is changing the real byte values, causing
            problem when sending and receiving keys
             */
        }

        this.creation = creation;
    }

    /**
     * Get the packet ID
     *
     * @return the packet id
     */
    @Override
    public int id() {
        return ownerId;
    }

    /**
     * Get the packet timestamp
     *
     * @return the packet timestamp
     */
    @Override
    public Instant timestamp() {
        return creation;
    }

    /**
     * Get the frame unique id
     *
     * @return the frame unique id
     */
    @Override
    public UUID uniqueId() {
        return frameId;
    }

    /**
     * Get this frame position
     *
     * @return the frame position
     */
    @Override
    public int position() {
        return position;
    }

    /**
     * Get the amount of frames for this
     * packet frame
     *
     * @return the amount of frames
     */
    @Override
    public int frames() {
        return frameCount;
    }

    /**
     * Get the frame length
     *
     * @return the frame length
     */
    @Override
    public int length() {
        return byteMap.size();
    }

    /**
     * Read the frame data
     *
     * @param output the output data
     * @param index  the start index
     */
    @Override
    public void read(final byte[] output, final int index) {
        if (output.length == byteMap.size()) {
            byte[] byteCreator = new byte[byteMap.size()];
            for (int i = 0; i < byteMap.size(); i++) {
                String encoded = byteMap.get(i);
                byte decoded = Byte.parseByte(encoded);
                byteCreator[i] = decoded;

                //plugin.info("[Frame] Decoded {0} to {1}({2})", i, encoded, decoded);
            }

            System.arraycopy(byteCreator, 0, output, index, byteCreator.length);
        } else {
            for (int i = 0; i < output.length; i++) {
                if (i < byteMap.size()) {
                    String encoded = byteMap.get(i);
                    byte decoded = Byte.parseByte(encoded);
                    output[i] = decoded;

                    //plugin.info("[Frame] Decoded {0} to {1}({2})", i, encoded, decoded);
                    continue;
                }

                break;
            }
        }
    }
}
