package es.karmadev.locklogin.common.api.packet.frame;

import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.FrameBuilder;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

public class CFrameBuilder implements FrameBuilder, Comparator<PacketFrame> {

    private final List<PacketFrame> frames = new CopyOnWriteArrayList<>();
    private Function<byte[], byte[]> onDecryptRequest = (b) -> b;

    public CFrameBuilder() {}

    public CFrameBuilder(final Function<byte[], byte[]> onDecryptRequest) {
        this.onDecryptRequest = onDecryptRequest;
    }

    /**
     * Append a frame to the builder
     *
     * @param frame the frame to append
     */
    @Override
    public void append(final PacketFrame frame) {
        frames.add(frame);
    }

    /**
     * Build the packet from the frames
     *
     * @return the packet
     */
    @Override
    public byte[] build() {
        frames.sort(this);
        byte[] completeData = new byte[0];
        for (PacketFrame frame : frames) {
            int startPos = completeData.length;

            byte[] tData = new byte[frame.length()];
            frame.read(tData, 0);
            tData = onDecryptRequest.apply(tData);

            completeData = Arrays.copyOf(completeData, completeData.length + tData.length);
            System.arraycopy(tData, 0, completeData, startPos, tData.length);
        }

        return completeData;
    }

    /**
     * Split the packet into packet frames
     *
     * @param packet    the packet to split
     * @param rawPacket the packet to split
     * @return the packet frames
     */
    @Override
    public PacketFrame[] split(final OutgoingPacket packet, final byte[] rawPacket) {
        if (rawPacket.length >= 245) {
            PacketFrame[] frames = new PacketFrame[0];
            List<byte[]> bytes = splitByte(rawPacket, 245);
            for (int i = 0; i < bytes.size(); i++) {
                CFramePacket frame = new CFramePacket(packet.id(), i, bytes.size(), bytes.get(i), packet.timestamp());
                frames = Arrays.copyOf(frames, frames.length + 1);
                frames[frames.length - 1] = frame;
            }

            return frames;
        } else {
            return new PacketFrame[]{new CFramePacket(packet.id(), 0, 1, rawPacket, packet.timestamp())};
        }
    }

    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     * <p>
     * The implementor must ensure that {@link Integer#signum
     * signum}{@code (compare(x, y)) == -signum(compare(y, x))} for
     * all {@code x} and {@code y}.  (This implies that {@code
     * compare(x, y)} must throw an exception if and only if {@code
     * compare(y, x)} throws an exception.)<p>
     * <p>
     * The implementor must also ensure that the relation is transitive:
     * {@code ((compare(x, y)>0) && (compare(y, z)>0))} implies
     * {@code compare(x, z)>0}.<p>
     * <p>
     * Finally, the implementor must ensure that {@code compare(x,
     * y)==0} implies that {@code signum(compare(x,
     * z))==signum(compare(y, z))} for all {@code z}.
     *
     * @param first the first object to be compared.
     * @param second the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the
     * first argument is less than, equal to, or greater than the
     * second.
     * @throws NullPointerException if an argument is null and this
     *                              comparator does not permit null arguments
     * @throws ClassCastException   if the arguments' types prevent them from
     *                              being compared by this comparator.
     * @apiNote It is generally the case, but <i>not</i> strictly required that
     * {@code (compare(x, y)==0) == (x.equals(y))}.  Generally speaking,
     * any comparator that violates this condition should clearly indicate
     * this fact.  The recommended language is "Note: this comparator
     * imposes orderings that are inconsistent with equals."
     */
    @Override
    public int compare(final PacketFrame first, final PacketFrame second) {
        return Integer.compare(first.position(), second.position());
    }

    private List<byte[]> splitByte(final byte[] raw, final int length) {
        byte[] byteWriter = new byte[0];
        List<byte[]> bytes = new ArrayList<>();
        int virtualIndex = 0;
        for (int i = 0; i <= length; i++) {
            if (virtualIndex == raw.length) break;
            if (i == length) {
                i = 0;
                bytes.add(byteWriter);
                byteWriter = new byte[0];
            }

            byteWriter = Arrays.copyOf(byteWriter, byteWriter.length + 1);
            byteWriter[byteWriter.length - 1] = raw[virtualIndex++];
        }

        return bytes;
    }
}
