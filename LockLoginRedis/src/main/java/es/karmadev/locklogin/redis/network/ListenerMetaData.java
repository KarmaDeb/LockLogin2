package es.karmadev.locklogin.redis.network;

import es.karmadev.locklogin.api.network.communication.packet.listener.NetworkListener;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.NetworkEvent;
import lombok.Getter;
import lombok.Value;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Value(staticConstructor = "of")
@Getter
class ListenerMetaData {

    NetworkListener listener;
    Map<Class<? extends NetworkEvent>, Set<MethodHandle>> invokers = new HashMap<>();

    public MethodHandle[] getHandlers(final NetworkEvent event) {
        return invokers.getOrDefault(event.getClass(), Collections.emptySet()).toArray(new MethodHandle[0]);
    }
}
