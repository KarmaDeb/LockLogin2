package es.karmadev.locklogin.common.plugin.web.local;

import es.karmadev.api.core.KarmaAPI;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.ModuleLoader;
import es.karmadev.locklogin.api.extension.module.exception.InvalidModuleException;
import es.karmadev.locklogin.api.extension.module.lang.ModulePhrases;
import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.plugin.marketplace.storage.StoredResource;
import es.karmadev.locklogin.common.plugin.web.manifest.ManifestFile;
import es.karmadev.locklogin.common.plugin.web.manifest.ResourceManifest;
import es.karmadev.locklogin.common.util.LockLoginJson;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Value(staticConstructor = "of")
@Getter
public class CStoredResource implements StoredResource {

    int id;

    @NonFinal
    boolean loaded;

    Category category;
    String name;
    String description;
    String publisher;
    String version;
    Instant downloadDate;

    @FieldNameConstants.Exclude
    ResourceManifest manifest;

    /**
     * Loads the resource, for translations, this
     * will make the plugin to switch to the translation,
     * meanwhile for modules, will make the module
     * to load
     */
    @Override
    public void load() {
        if (loaded) return;

        LockLogin plugin = CurrentPlugin.getPlugin();
        Path resourceDirectory = plugin.workingDirectory().resolve("marketplace").resolve("resources").resolve(String.valueOf(id));

        boolean success = false;
        if (category.equals(Category.TRANSLATION)) {
            if (manifest.getVersion() < LockLoginJson.getLangVersion()) {
                plugin.warn("Couldn't load translation {0} because the manifest lang version {1} is less than the current language version {2}",
                        name, manifest.getVersion(), LockLoginJson.getLangVersion());
                return;
            }

            plugin.info("Preparing to load translations of {0}", manifest.getTitle());
            for (ManifestFile file : manifest.getFiles()) {
                String flName = file.getFile();
                Path fileEntry = resourceDirectory.resolve(file.getFile());
                if (flName.contains("/")) {
                    fileEntry = resourceDirectory;
                    for (String p : flName.split("/")) {
                        fileEntry = fileEntry.resolve(p);
                    }
                }

                if (Files.exists(fileEntry)) {
                    Path targetDirectory = plugin.workingDirectory().resolve("marketplace").resolve("translation").resolve(file.getDirectoryName());
                    PathUtilities.createDirectory(targetDirectory);

                    Path langDestination = targetDirectory.resolve("messages.yml");
                    if (!Files.exists(langDestination)) {
                        PathUtilities.write(langDestination, PathUtilities.read(fileEntry));
                    }

                    for (String name : file.getLocaleNames()) {
                        if (plugin.languagePackManager().load(name, targetDirectory)) {
                            plugin.info("Added translation {0}", name);
                            success = true;
                        } else {
                            plugin.warn("Failed to add translation {0}, because it's probably already added by another translation", name);
                        }
                    }
                }
            }
        } else {
            ModuleLoader loader = plugin.moduleManager().loader();
            plugin.info("Preparing to load modules of {0}", manifest.getTitle());

            Map<String, Path> locales = new HashMap<>();
            for (ManifestFile file : manifest.getFiles()) {
                String flName = file.getFile();
                Path fileEntry = resourceDirectory.resolve(file.getFile());
                if (flName.contains("/")) {
                    fileEntry = resourceDirectory;
                    for (String p : flName.split("/")) {
                        fileEntry = fileEntry.resolve(p);
                    }
                }

                String extension = PathUtilities.getExtension(fileEntry);
                if (extension.equals("yml")) {
                    Path targetDirectory = plugin.workingDirectory().resolve("marketplace").resolve("modules")
                            .resolve("translation").resolve(file.getDirectoryName());
                    PathUtilities.createDirectory(targetDirectory);

                    Path langDestination = targetDirectory.resolve(PathUtilities.getName(fileEntry, true));
                    if (!Files.exists(langDestination)) {
                        PathUtilities.write(langDestination, PathUtilities.read(fileEntry));
                    }

                    for (String name : file.getLocaleNames()) {
                        locales.put(name, langDestination);
                    }
                }
            }

            for (ManifestFile file : manifest.getFiles()) {
                String flName = file.getFile();
                Path fileEntry = resourceDirectory.resolve(file.getFile());
                if (flName.contains("/")) {
                    fileEntry = resourceDirectory;
                    for (String p : flName.split("/")) {
                        fileEntry = fileEntry.resolve(p);
                    }
                }

                String extension = PathUtilities.getExtension(fileEntry);
                if (extension.equals("jar")) {
                    try (JarFile jar = new JarFile(fileEntry.toFile())) {
                        JarEntry moduleYML = jar.getJarEntry("module.yml");

                        if (moduleYML != null) {
                            Path targetModule = plugin.workingDirectory().resolve("marketplace").resolve("modules")
                                    .resolve("addon").resolve(file.getDirectoryName());

                            PathUtilities.createDirectory(targetModule.getParent());

                            if (!Files.exists(targetModule)) {
                                try {
                                    Files.copy(fileEntry, targetModule);
                                } catch (Exception ex) {
                                    plugin.log(ex, "Failed to prepare resource load {0}", PathUtilities.pathString(targetModule));
                                    plugin.warn("Failed to prepare resource load {0}", PathUtilities.pathString(targetModule));
                                    continue;
                                }
                            }

                            try {
                                Module module = loader.load(targetModule);
                                for (String name : locales.keySet()) {
                                    plugin.languagePackManager().load(module, name, locales.get(name));
                                }

                                if (loader.enable(module)) {
                                    plugin.info("Successfully enabled module {0}", module.getName());
                                    for (String name : locales.keySet()) {
                                        String lang = plugin.languagePackManager().getName(module);
                                        plugin.languagePackManager().setLang(module, name);
                                        ModulePhrases phrases = plugin.languagePackManager().getMessenger(module);

                                        if (phrases == null) {
                                            plugin.warn("Module {0} provided a language pack {1} but the module does not initialize it", module.getName(), name);
                                        }

                                        plugin.languagePackManager().setLang(module, lang);
                                    }
                                } else {
                                    plugin.warn("Failed to enable module {0}", module.getName());
                                }
                            } catch (InvalidModuleException ex) {
                                plugin.log(ex, "Failed to load module from resource {0}", manifest.getTitle());
                                plugin.warn("Couldn't load module {0} from resource {1}", PathUtilities.pathString(targetModule), name);
                            }
                        } else {
                            KarmaAPI.inject(fileEntry, plugin.plugin().getClass().getClassLoader());
                            plugin.info("Successfully injected resource {0} from resource {1}", PathUtilities.pathString(fileEntry), name);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        loaded = success;
    }

    /**
     * Unloads the resource, for translations, this
     * will make the plugin switch to the default
     * language, meanwhile for modules, will make
     * the module to unload
     */
    @Override
    public void unload() {
        if (!loaded) return;

        LockLogin plugin = CurrentPlugin.getPlugin();

        if (category.equals(Category.TRANSLATION)) {
            plugin.info("Preparing to unload translations of {0}", manifest.getTitle());

            for (ManifestFile file : manifest.getFiles()) {
                Path targetDirectory = plugin.workingDirectory().resolve("marketplace").resolve("translation")
                        .resolve(file.getDirectoryName()).resolve("messages.yml");

                if (Files.exists(targetDirectory)) {
                    PathUtilities.destroy(targetDirectory);
                }

                for (String lang : file.getLocaleNames()) {
                    plugin.languagePackManager().unload(lang);
                    plugin.info("Unloaded translation {0}", lang);
                }
            }
        } else {
            ModuleLoader loader = plugin.moduleManager().loader();
            plugin.info("Preparing to unload modules of {0}", manifest.getTitle());

            Set<String> locales = new HashSet<>();
            for (ManifestFile file : manifest.getFiles()) {
                String flName = file.getFile();
                String extension = PathUtilities.getExtension(flName);

                if (extension.equals("yml")) {
                    Path targetDirectory = plugin.workingDirectory().resolve("marketplace").resolve("modules")
                            .resolve("translation").resolve(file.getDirectoryName());
                    Path langDestination = targetDirectory.resolve(file.getFile());

                    if (Files.exists(langDestination)) {
                        PathUtilities.destroy(langDestination);
                    }

                    locales.addAll(file.getLocaleNames());
                }
            }

            for (ManifestFile file : manifest.getFiles()) {
                String flName = file.getFile();
                String extension = PathUtilities.getExtension(flName);

                if (extension.equals("jar")) {
                    Path targetModule = plugin.workingDirectory().resolve("marketplace").resolve("modules")
                            .resolve("addon").resolve(file.getDirectoryName());

                    Module module = loader.getModule(targetModule);
                    if (module != null) {
                        if (module.isEnabled()) {
                            loader.unload(module);
                        }
                    }

                    for (String name : locales) {
                        plugin.languagePackManager().unload(module, name);
                    }

                    if (Files.exists(targetModule)) {
                        PathUtilities.destroy(targetModule);
                    }
                }
            }
        }

        loaded = false;
    }
}
