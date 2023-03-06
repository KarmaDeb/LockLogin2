package es.karmadev.locklogin.api.event;

/**
 * Event handler
 */
public interface EventHandler {

    /**
     * Ony any event listener
     *
     * @param event the event
     */
    default void onAny(final LockLoginEvent event) {}
}
