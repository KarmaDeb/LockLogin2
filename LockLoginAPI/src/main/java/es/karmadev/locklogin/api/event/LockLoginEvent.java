package es.karmadev.locklogin.api.event;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

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
        if (caller == null) {
            LockLogin plugin = CurrentPlugin.getPlugin();
            plugin.getRuntime().verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY, LockLoginEvent.class, "LockLoginEvent(Module)");
        }
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
