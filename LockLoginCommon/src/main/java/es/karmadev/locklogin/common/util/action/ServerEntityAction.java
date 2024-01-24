package es.karmadev.locklogin.common.util.action;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.common.util.ActionListener;
import lombok.Builder;

import java.util.function.Consumer;

@Builder
public class ServerEntityAction implements ActionListener<LocalNetworkClient> {

    private final Consumer<LocalNetworkClient> onClientConnect;
    private final Consumer<LocalNetworkClient> onClientDisconnect;
    private final Consumer<NetworkClient> onOnlineConnect;
    private final Consumer<NetworkClient> onOnlineDisconnect;

    /**
     * When an action is performed
     *
     * @param id       the action id
     * @param consumer the action consumer
     */
    @Override
    public void onAction(final String id, final LocalNetworkClient consumer) {
        switch (id.toLowerCase()) {
            case "offline_connect":
                if (onClientConnect != null) onClientConnect.accept(consumer);
                break;
            case "offline_disconnect":
                if (onClientDisconnect != null) onClientDisconnect.accept(consumer);
                break;
            case "online_connect":
                NetworkClient onlineConnectedClient = consumer.client();
                if (onOnlineConnect != null && onlineConnectedClient != null) onOnlineConnect.accept(onlineConnectedClient);
                break;
            case "online_disconnect":
                NetworkClient onlineDisconnectedClient = consumer.client();
                if (onOnlineDisconnect != null && onlineDisconnectedClient != null) onOnlineDisconnect.accept(onlineDisconnectedClient);
                break;
        }
    }

    public final static String OFF_CONN = "offline_connect";
    public final static String OFF_DIS = "offline_disconnect";
    public final static String ON_CONN = "online_connect";
    public final static String ON_DIS = "online_disconnect";
}
