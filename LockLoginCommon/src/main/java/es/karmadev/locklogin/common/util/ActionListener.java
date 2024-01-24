package es.karmadev.locklogin.common.util;

/**
 * Action listener
 *
 * @param <T> the action container
 */
public interface ActionListener<T> {

    /**
     * When an action is performed
     *
     * @param id the action id
     * @param consumer the action consumer
     */
    void onAction(final String id, final T consumer);
}
