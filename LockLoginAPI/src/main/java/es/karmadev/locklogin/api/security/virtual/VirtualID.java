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
     * Virtualize the input
     *
     * @param input the input to virtualize
     * @param reference the references to force
     * @return the virtualized input
     *
     * @throws IllegalStateException if the input doesn't match the references length
     */
    VirtualizedInput virtualize(final String input, final int[] reference) throws IllegalStateException;
}
