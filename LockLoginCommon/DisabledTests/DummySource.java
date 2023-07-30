package es.karmadev.locklogin.test;

import es.karmadev.api.core.CoreModule;
import es.karmadev.api.core.source.KarmaSource;
import es.karmadev.api.strings.placeholder.PlaceholderEngine;
import es.karmadev.api.version.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public class DummySource extends KarmaSource {

    public DummySource() {
        super("LockLogin", Version.of(1, 0, 0), "", "KarmaDev");
    }

    @Override
    public void start() {

    }

    @Override
    public void kill() {

    }

    @Override
    public @NotNull String identifier() {
        return null;
    }

    @Override
    public @Nullable URI sourceUpdateURI() {
        return null;
    }

    @Override
    public @NotNull PlaceholderEngine placeholderEngine(final String s) {
        return null;
    }

    @Override
    public @Nullable CoreModule getModule(final String s) {
        return null;
    }

    @Override
    public boolean registerModule(CoreModule coreModule) {
        return false;
    }

    @Override
    public void loadIdentifier(String s) {

    }

    @Override
    public void saveIdentifier(String s) {

    }
}
