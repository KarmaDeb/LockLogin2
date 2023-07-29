package es.karmadev.locklogin.common.api.extension.loader;

import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.locklogin.api.extension.Module;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class finder
 */
class CModData {

    private final Set<Class<?>> classes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Getter
    private final Path file;
    @Getter
    private final Module module;

    @Getter
    private final YamlFileHandler moduleYML;

    public CModData(final Path file, final Module module, final YamlFileHandler yaml) {
        this.file = file;
        this.module = module;
        this.moduleYML = yaml;
    }

    public void assign(final Class<?>... classes) {
        if (classes == null) return;
        this.classes.addAll(Arrays.asList(classes));
    }

    public boolean ownsClass(final Class<?> clazz) {
        return classes.contains(clazz);
    }
}
