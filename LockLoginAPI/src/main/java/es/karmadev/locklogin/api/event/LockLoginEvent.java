package es.karmadev.locklogin.api.event;

import es.karmadev.locklogin.api.extension.module.Module;

/**
 * LockLogin event
 */
@SuppressWarnings("unused")
public abstract class LockLoginEvent {

    private final Module caller;

    /**
     * Initialize the event
     *
     * @throws SecurityException if the event initializer has no
     * module and the initializer is not a plugin
     */
    public LockLoginEvent() throws SecurityException {
        this(null);
    }

    /**
     * Initialize the event
     *
     * @param caller the event caller
     * @throws SecurityException if the event initializer has no
     * module and the initializer is not a plugin
     */
    public LockLoginEvent(final Module caller) throws SecurityException {
        this.caller = caller;
    }

    /**
     * Get the event caller
     *
     * @return the event caller
     */
    public final Module getCaller() {
        return caller;
    }
}
