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
     * @param refference the refferences to force
     * @return the vritualized input
     *
     * @throws IllegalStateException if the input doesn't match the refferences length
     */
    VirtualizedInput virtualize(final String input, final int[] refference) throws IllegalStateException;
}
