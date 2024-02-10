package es.karmadev.locklogin.spigot.api;

import org.bukkit.Bukkit;

public abstract class NMSPayload {

    private static NMSPayload factory;

    protected void registerPayload() {
        NMSPayload.factory = this;
    }

    /**
     * Execute an auto-detection for the NMS
     * payload
     * @throws RuntimeException if the detection fails
     */
    public static void autoDetect() throws RuntimeException {
        String nmsVersion = Bukkit.getServer()
                .getClass()
                .getPackage()
                .getName()
                .split("\\.")[3];

        try {
            Class<?> clazz = Class.forName(String.format("es.karmadev.locklogin.spigot.nms.%s.CustomPayloadMaker",
                    nmsVersion));
            clazz.getConstructor().newInstance();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static NMSPayload getFactory() {
        return NMSPayload.factory;
    }

    /**
     * Get if the custom payload is
     * supported
     *
     * @return the supported status
     */
    public static boolean isNotSupported() {
        return NMSPayload.factory == null ||
                NMSPayload.factory.createPacket("", new byte[0]) == null;
    }

    /**
     * Create a packet
     *
     * @param namespace the packet namespace
     * @param data the packet data
     * @return the packet
     */
    public abstract PayloadPacket createPacket(final String namespace, final byte[] data);
}
