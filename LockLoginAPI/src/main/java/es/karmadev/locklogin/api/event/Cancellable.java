package es.karmadev.locklogin.api.event;

/**
 * Cancellable event
 */
public interface Cancellable {

    /**
     * Cancel the event
     *
     * @param cancel if the event is cancelled
     * @param reason the cancel reason
     */
    void setCancelled(final boolean cancel, final String reason);

    /**
     * Get if the event is cancelled
     *
     * @return if the event is cancelled
     */
    boolean isCancelled();

    /**
     * Get the event cancel reason
     *
     * @return the cancel reason
     */
    String cancelReason();
}
