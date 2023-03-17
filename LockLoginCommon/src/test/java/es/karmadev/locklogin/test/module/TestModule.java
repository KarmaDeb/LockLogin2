package es.karmadev.locklogin.test.module;

import es.karmadev.locklogin.api.extension.Module;

public class TestModule extends Module {

    /**
     * When the module gets loaded
     */
    @Override
    public void onLoad() {

    }

    /**
     * When the module gets disabled
     */
    @Override
    public void onUnload() {

    }

    /**
     * Karma source update URL
     *
     * @return the source update URL
     */
    @Override
    public String updateURL() {
        return null;
    }
}
