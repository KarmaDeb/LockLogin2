package es.karmadev.locklogin.test.module;

import es.karmadev.api.core.CoreModule;
import es.karmadev.api.core.source.runtime.SourceRuntime;
import es.karmadev.api.file.util.NamedStream;
import es.karmadev.api.logger.SourceLogger;
import es.karmadev.api.schedule.task.TaskScheduler;
import es.karmadev.api.strings.StringFilter;
import es.karmadev.api.strings.placeholder.PlaceholderEngine;
import es.karmadev.locklogin.api.extension.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;

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

    @Override
    public @NotNull String identifier() {
        return null;
    }

    @Override
    public @Nullable URI sourceUpdateURI() {
        return null;
    }

    @Override
    public @NotNull SourceRuntime runtime() {
        return null;
    }

    @Override
    public @NotNull PlaceholderEngine placeholderEngine(String s) {
        return null;
    }

    @Override
    public @NotNull TaskScheduler scheduler(String s) {
        return null;
    }

    @Override
    public @NotNull Path workingDirectory() {
        return null;
    }

    @Override
    public @NotNull Path navigate(String s, String... strings) {
        return null;
    }

    @Override
    public @Nullable NamedStream findResource(String s) {
        return null;
    }

    @Override
    public @NotNull NamedStream[] findResources(String s, @Nullable StringFilter stringFilter) {
        return new NamedStream[0];
    }

    @Override
    public boolean export(String s, Path path) {
        return false;
    }

    @Override
    public SourceLogger logger() {
        return null;
    }

    @Override
    public @Nullable CoreModule getModule(String s) {
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
