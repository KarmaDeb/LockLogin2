package es.karmadev.locklogin.api.user;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;

import java.util.UUID;

/**
 * LockLogin user factory
 */
public interface UserFactory<T extends LocalNetworkClient> {

    /**
     * Create a new user
     *
     * @param name the user name
     * @param uniqueId the user unique id
     * @param account the user account
     * @param session the user session
     * @return the new created user
     */
    T create(final String name, final UUID uniqueId, final int account, final int session);

    /**
     * Create an user without an account and
     * session
     *
     * @param name the user name
     * @param uniqueId the user unique id
     * @return the new created user
     */
    T create(final String name, final UUID uniqueId);
}
