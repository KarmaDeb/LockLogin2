package es.karmadev.locklogin.common.plugin.secure.totp;

import es.karmadev.locklogin.api.network.client.NetworkClient;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

/**
 * Represents a global handler for internal
 * TOTP events (when a client types in the TOTP
 * code successfully)
 */
public class TotpGlobalHandler {

    private final ConcurrentMap<NetworkClient, Set<TotpHandler>> handlers = new ConcurrentHashMap<>();

    /**
     * Add a new handler
     *
     * @param client the client that the handler
     *               has been requested for
     * @param handler the handler to add
     */
    public void addHandler(final NetworkClient client, final BiConsumer<Boolean, TotpHandler> handler) {
        TotpHandler vHandler = new TotpHandler(){
            /**
             * Destroy the handler
             */
            @Override
            public void destroy() {
                Set<TotpHandler> handlers = TotpGlobalHandler.this.handlers.computeIfAbsent(client, (set) -> ConcurrentHashMap.newKeySet());
                handlers.remove(this);
            }

            /**
             * Handle a TOTP action
             *
             * @param wasSuccess if the action was successful
             */
            @Override
            public void handle(final boolean wasSuccess) {
                handler.accept(wasSuccess, this);
            }
        };

        Set<TotpHandler> handlers = TotpGlobalHandler.this.handlers.computeIfAbsent(client, (set) -> ConcurrentHashMap.newKeySet());
        handlers.add(vHandler);
    }

    /**
     * Trigger a TOTP event
     *
     * @param client the client that requested the
     *               trigger
     * @param isSuccess if the trigger is successful
     */
    public void trigger(final NetworkClient client, final boolean isSuccess) {
        handlers.forEach((cl, handler) -> {
            if (!cl.equals(client)) return;
            handler.forEach((h) -> h.handle(isSuccess));
        });
    }

    /**
     * Destroy all the handlers attached to
     * the client
     *
     * @param client the client to remove handlers from
     */
    public void destroyAll(final NetworkClient client) {
        handlers.remove(client);
    }
}
