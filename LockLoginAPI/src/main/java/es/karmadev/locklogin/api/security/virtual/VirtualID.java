package es.karmadev.locklogin.api.security.virtual;

/**
 * LockLogin virtual ID
 */
public interface VirtualID {

    /**
     * Virtualize the input
     *
     * @param input the input to virtualize
     * @return the virtualized input
     */
    VirtualizedInput virtualize(final String input);

    /**
     * Un-virtualize the input
     *
     * @return the raw input
     */
    String resolve(VirtualizedInput input);
}
