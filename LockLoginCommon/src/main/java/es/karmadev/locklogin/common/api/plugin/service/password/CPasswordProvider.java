package es.karmadev.locklogin.common.api.plugin.service.password;

import es.karmadev.locklogin.api.plugin.service.ServiceProvider;

public class CPasswordProvider implements ServiceProvider<CPasswordValidator> {

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "Password Validator Service";
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
    public CPasswordValidator serve(final Object... arguments) {
        if (arguments.length == 0 || arguments[0] == null) return null;
        CPasswordValidator validator = new CPasswordValidator(arguments[0].toString());
        validator.grantedThroughServiceProvider = true;

        return validator;
    }

    /**
     * Get the service class
     *
     * @return the service class
     */
    @Override
    public Class<CPasswordValidator> getService() {
        return CPasswordValidator.class;
    }
}
