package es.karmadev.locklogin.common.api.plugin.service.name;

import es.karmadev.locklogin.api.plugin.service.ServiceProvider;

public class CNameProvider implements ServiceProvider<CNameValidator> {

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "Name Validator Provider";
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
    public CNameValidator serve(final Object... arguments) {
        if (arguments.length == 0 || arguments[0] == null) return null;
        CNameValidator validator = new CNameValidator(arguments[0].toString());
        validator.grantedThroughService = true;

        return validator;
    }

    /**
     * Get the service class
     *
     * @return the service class
     */
    @Override
    public Class<CNameValidator> getService() {
        return CNameValidator.class;
    }
}
