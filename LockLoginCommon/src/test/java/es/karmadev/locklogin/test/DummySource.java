package es.karmadev.locklogin.test;

import ml.karmaconfigs.api.common.karma.source.KarmaSource;

public class DummySource implements KarmaSource {


    /**
     * Karma source name
     *
     * @return the source name
     */
    @Override
    public String name() {
        return "LockLogin";
    }

    /**
     * Karma source version
     *
     * @return the source version
     */
    @Override
    public String version() {
        return "1.0.0";
    }

    /**
     * Karma source description
     *
     * @return the source description
     */
    @Override
    public String description() {
        return "";
    }

    /**
     * Karma source authors
     *
     * @return the source authors
     */
    @Override
    public String[] authors() {
        return new String[]{
                "KarmaDev"
        };
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
