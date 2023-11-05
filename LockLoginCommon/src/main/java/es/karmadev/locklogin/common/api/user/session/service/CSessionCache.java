package es.karmadev.locklogin.common.api.user.session.service;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.session.service.SessionCache;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;

@AllArgsConstructor @Getter
public class CSessionCache implements SessionCache {

    private final LocalNetworkClient client;
    private final InetSocketAddress address;
    private final boolean logged;
    private final boolean totpLogged;
    private final boolean pinLogged;
}
