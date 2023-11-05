package es.karmadev.locklogin.common.api.user.session.service;

import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;

public class SessionStoreProvider implements ServiceProvider<CSessionStore> {

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "session store provider";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return false;
    }

    /**
     * Serve a plugin service
     *
     * @param arguments the service arguments
     * @return the plugin service
     */
    @Override
    public CSessionStore serve(final Object... arguments) {
        for (Object argument : arguments) {
            if (argument instanceof SQLDriver) {
                SQLDriver driver = (SQLDriver) argument;
                CSessionStore store = new CSessionStore(driver);
                store.grantedThroughServiceProvider = true;
                //Enable the service for being used

                return store;
            }
        }

        return null;
    }

    /**
     * Get the service class
     *
     * @return the service class
     */
    @Override
    public Class<CSessionStore> getService() {
        return CSessionStore.class;
    }
}
