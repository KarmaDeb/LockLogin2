package es.karmadev.locklogin.test;

import es.karmadev.locklogin.api.event.entity.client.EntityCreatedEvent;
import es.karmadev.locklogin.api.event.handler.EventHandler;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

@SuppressWarnings("unused")
public class TestListener implements EventHandler {

    public void onEntityCreate(final EntityCreatedEvent e) {
        LocalNetworkClient client = e.getEntity();
        System.out.println("Created: " + client.id());
    }
}
