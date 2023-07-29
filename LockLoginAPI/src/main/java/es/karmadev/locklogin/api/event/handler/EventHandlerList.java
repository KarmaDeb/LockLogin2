package es.karmadev.locklogin.api.event.handler;

import es.karmadev.locklogin.api.extension.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler list
 */
@SuppressWarnings("unused")
public final class EventHandlerList {

    private final Map<Module, List<EventHandler>> handlers = new ConcurrentHashMap<>();

    /**
     * Initialize the event handler list. Usually this is initialized
     * on the same field definition of your {@link es.karmadev.locklogin.api.event.LockLoginEvent event}
     */
    public EventHandlerList() {}

    /**
     * Get all the module handlers for this
     * event registered under the specified module
     *
     * @param module the module
     * @return the event handlers of the module
     */
    public EventHandler[] getHandlers(final Module module) {
        List<EventHandler> data = handlers.getOrDefault(module, new ArrayList<>());
        return data.toArray(new EventHandler[0]);
    }

    /**
     * Unregister the specified event
     * listener
     *
     * @param handler the event listener
     */
    public void unregister(final EventHandler handler) {
        Set<Module> modules = handlers.keySet();

        for (Module module : modules) {
            List<EventHandler> data = handlers.getOrDefault(module, new ArrayList<>());
            data.remove(handler);

            handlers.put(module, data);
        }
    }

    /**
     * Unregister all the event listeners
     *
     * @param module the module
     */
    public void unregisterAll(final Module module) {
        handlers.remove(module);
    }

    /**
     * Assign an event handler on this module
     *
     * @param handler the event handler
     * @param module the module
     */
    public void addHandler(final EventHandler handler, final Module module) {
        List<EventHandler> data = handlers.getOrDefault(module, new ArrayList<>());
        data.add(handler);

        handlers.put(module, data);
    }
}
