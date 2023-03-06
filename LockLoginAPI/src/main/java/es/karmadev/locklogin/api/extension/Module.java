package es.karmadev.locklogin.api.extension;

import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * LockLogin module
 */
public interface Module extends KarmaSource {

    /**
     * When the module gets loaded
     */
    void onLoad();

    /**
     * When the module gets disabled
     */
    void onUnload();

    /**
     * Get the LockLogin plugin
     *
     * @return the plugin
     */
    LockLogin plugin();

    /**
     * Karma source name
     *
     * @return the source name
     */
    @Override
    default String name() {
        File file = getSourceFile();

        try(JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("module.yml");

            if (entry != null && !entry.isDirectory()) {
                try (InputStream stream = jar.getInputStream(entry)) {
                    if (stream != null) {
                        KarmaYamlManager yaml = new KarmaYamlManager(stream);
                        if (yaml.isSet("name")) {
                            return yaml.getString("name");
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Karma source version
     *
     * @return the source version
     */
    @Override
    default String version() {
        File file = getSourceFile();

        try(JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("module.yml");

            if (entry != null && !entry.isDirectory()) {
                try (InputStream stream = jar.getInputStream(entry)) {
                    if (stream != null) {
                        KarmaYamlManager yaml = new KarmaYamlManager(stream);
                        if (yaml.isSet("version")) {
                            return yaml.getString("version");
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Karma source description
     *
     * @return the source description
     */
    @Override
    default String description() {
        File file = getSourceFile();

        try(JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("module.yml");

            if (entry != null && !entry.isDirectory()) {
                try (InputStream stream = jar.getInputStream(entry)) {
                    if (stream != null) {
                        KarmaYamlManager yaml = new KarmaYamlManager(stream);
                        if (yaml.isSet("description")) {
                            Object value = yaml.get("description");
                            if (value instanceof String) {
                                return (String) value;
                            }

                            if (value instanceof List) {
                                List<?> unknownList = (List<?>) value;
                                StringBuilder builder = new StringBuilder();
                                int index = 0;
                                for (Object object : unknownList) {
                                    builder.append(String.valueOf(object)).append((index++ != unknownList.size() - 1 ? " " : ""));
                                }

                                return builder.toString();
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Karma source authors
     *
     * @return the source authors
     */
    @Override
    default String[] authors() {
        File file = getSourceFile();

        try(JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("module.yml");

            if (entry != null && !entry.isDirectory()) {
                try (InputStream stream = jar.getInputStream(entry)) {
                    if (stream != null) {
                        KarmaYamlManager yaml = new KarmaYamlManager(stream);
                        if (yaml.isSet("description")) {
                            Object value = yaml.get("description");
                            if (value instanceof String) {
                                return new String[]{ (String) value };
                            }

                            if (value instanceof List) {
                                List<?> unknownList = (List<?>) value;
                                List<String> authorList = new ArrayList<>();
                                for (Object object : unknownList) {
                                    authorList.add(String.valueOf(object));
                                }

                                return authorList.toArray(new String[0]);
                            }

                            return new String[]{};
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
