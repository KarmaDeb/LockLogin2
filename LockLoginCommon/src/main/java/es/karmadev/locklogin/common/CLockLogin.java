package es.karmadev.locklogin.common;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.kson.JsonInstance;
import es.karmadev.api.kson.io.JsonReader;
import es.karmadev.api.logger.log.console.LogLevel;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.web.url.URLUtilities;
import es.karmadev.api.web.url.domain.WebDomain;
import es.karmadev.locklogin.api.BuildType;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.ModuleLoader;
import es.karmadev.locklogin.api.extension.module.exception.InvalidModuleException;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.AccountField;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.account.migration.AccountMigrator;
import es.karmadev.locklogin.api.user.account.migration.Transitional;
import es.karmadev.locklogin.common.api.protection.type.*;
import es.karmadev.locklogin.common.api.user.storage.account.transiction.CTransitional;
import es.karmadev.locklogin.common.plugin.web.CMarketPlace;
import es.karmadev.locklogin.common.plugin.web.local.CStoredResource;
import es.karmadev.locklogin.common.plugin.web.manifest.ResourceManifest;
import es.karmadev.locklogin.common.util.LockLoginJson;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@AllArgsConstructor
public class CLockLogin {

    private final LockLogin plugin;
    private final Path workingDirectory;

    public void registerHash() {
        LockLoginHasher hasher = plugin.hasher();

        try {
            hasher.registerMethod(new SHA512Hash());
            hasher.registerMethod(new SHA256Hash());
            hasher.registerMethod(new BCryptHash());
            hasher.registerMethod(new Argon2I());
            hasher.registerMethod(new Argon2D());
            hasher.registerMethod(new Argon2ID());
        } catch (UnnamedHashException ex) {
            plugin.log(ex, "An error occurred while registering default plugin hashes");
        }
    }

    public void loadModules() {
        Path modsFolder = workingDirectory.resolve("mods");
        if (!Files.isDirectory(modsFolder)) PathUtilities.destroy(modsFolder);

        PathUtilities.createDirectory(modsFolder);
        try(Stream<Path> mods = Files.list(modsFolder).filter((path) -> !Files.isDirectory(path))) {
            ModuleLoader loader = plugin.moduleManager().loader();
            mods.forEach((modFile) -> {
                try {
                    Module module = loader.load(modFile);

                    if (loader.enable(module)) {
                        plugin.info("Loaded module {0}", module.getName());
                    } else {
                        plugin.warn("Failed to load module {0}", module.getName());
                    }
                } catch (InvalidModuleException ex) {
                    plugin.log(ex, "Failed to load file {0} as module", PathUtilities.getName(modFile));
                    //spigot.err("Failed to load file {0} as a module file. Does it contains a module.yml?", PathUtilities.pathString(modFile));
                }
            });
        } catch (IOException ex) {
            plugin.log(ex, "An error occurred while loading modules");
        }
    }

    public void migrateLegacyData() {
        Path legacyUserDirectory = workingDirectory.resolve("data").resolve("accounts");
        if (Files.exists(legacyUserDirectory) && Files.isDirectory(legacyUserDirectory)) {
            plugin.logInfo("Found legacy accounts folder, preparing to migrate existing data");

            try(Stream<Path> files = Files.list(legacyUserDirectory).filter((file) ->
                    !Files.isDirectory(file) && PathUtilities.getExtension(file).equals("lldb"))) {

                Pattern namePattern = Pattern.compile("'player' -> [\"'].*[\"']");
                Pattern passPattern = Pattern.compile("'password' -> [\"'].*[\"']");
                Pattern tokenPattern = Pattern.compile("'token' -> [\"'].*[\"']");
                Pattern pinPattern = Pattern.compile("'pin' -> [\"'].*[\"']");
                Pattern gAuthPattern = Pattern.compile("'2fa' -> [true-false]");
                Pattern panicPattern = Pattern.compile("'panic' -> [\"'].*[\"']");

                Path migratedDirectory = workingDirectory.resolve("cache").resolve("migrations");
                PathUtilities.createDirectory(migratedDirectory);

                files.forEachOrdered((file) -> {
                    String fileName = PathUtilities.getName(file, true);

                    Path migrationFile = migratedDirectory.resolve(fileName);
                    List<String> lines = PathUtilities.readAllLines(file);

                    String name = null;
                    String legacyPassword = null;
                    String gAuth = null;
                    String legacyPin = null;
                    boolean gAuthStatus = false;
                    String legacyPanic = null;

                    for (String line : lines) {
                        Matcher nameMatcher = namePattern.matcher(line);
                        Matcher passMatcher = passPattern.matcher(line);
                        Matcher tokenMatcher = tokenPattern.matcher(line);
                        Matcher pinMatcher = pinPattern.matcher(line);
                        Matcher authMatcher = gAuthPattern.matcher(line);
                        Matcher panicMatcher = panicPattern.matcher(line);

                        final int ending = line.length() - 1;
                        if (nameMatcher.find()) {
                            int being = nameMatcher.start() + "'player' -> '".length();
                            name = line.substring(being, ending);

                            continue;
                        }
                        if (passMatcher.find()) {
                            int being = passMatcher.start() + "'password' -> '".length();
                            legacyPassword = line.substring(being, ending);

                            continue;
                        }
                        if (tokenMatcher.find()) {
                            int being = tokenMatcher.start() + "'token' -> '".length();
                            gAuth = line.substring(being, ending);

                            continue;
                        }
                        if (pinMatcher.find()) {
                            int being = pinMatcher.start() + "'pin' -> '".length();
                            legacyPin = line.substring(being, ending);

                            continue;
                        }
                        if (authMatcher.find()) {
                            int being = authMatcher.start() + "'2fa' -> ".length();
                            String rawValue = line.substring(being, ending + 1);

                            gAuthStatus = Boolean.parseBoolean(rawValue);
                            continue;
                        }
                        if (panicMatcher.find()) {
                            int being = panicMatcher.start() + "'panic' -> '".length();
                            legacyPanic = line.substring(being, ending);
                        }
                    }

                    if (name == null || legacyPassword == null ||
                            gAuth == null || legacyPin == null || legacyPanic == null) {

                        plugin.logWarn("Ignoring migration of {0} because there's missing data [{1}]",
                                (name != null ? name : PathUtilities.getName(file, false)));
                        return;
                    }

                    UUID uniqueId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
                    Transitional legacy = CTransitional.fromLegacy(
                            name,
                            uniqueId,
                            legacyPassword,
                            legacyPin,
                            gAuth,
                            legacyPanic,
                            gAuthStatus
                    );

                    UserFactory<? extends LocalNetworkClient> userFactory = plugin.getUserFactory(true);
                    LocalNetworkClient client = userFactory.create(name, uniqueId);

                    AccountFactory<? extends UserAccount> factory = plugin.getAccountFactory(true);
                    AccountMigrator<? extends UserAccount> migrator = factory.migrator();

                    UserAccount migrated = migrator.migrate(client, legacy, AccountField.EMAIL);
                    if (migrated != null) {
                        plugin.logInfo("Successfully migrated account of {0}", name);
                        try {
                            Files.move(file, migrationFile, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            plugin.log(ex, "Failed to move legacy player {0} file into migrations folder {1}, account might be processed and migrated on the next server boot unless moved manually",
                                    PathUtilities.pathString(file, '/'), PathUtilities.pathString(migrationFile, '/'));
                        }
                    }
                });
            } catch (IOException ex) {
                plugin.log(ex, "Failed to migrate legacy accounts");
            }
        }
    }

    public void arrangeMarketplace(final CMarketPlace marketPlace) {
        Path resourcesDirectory = plugin.workingDirectory().resolve("marketplace").resolve("resources");
        if (!Files.exists(resourcesDirectory) &&
                !PathUtilities.createDirectory(resourcesDirectory)) return;

        try(Stream<Path> files = Files.list(resourcesDirectory).filter(Files::isDirectory)) {
            plugin.logInfo("Preparing to load marketplace resources");

            Pattern idPattern = Pattern.compile("id=[0-9]*");
            Pattern categoryPattern = Pattern.compile("category=[A-Z]*");
            Pattern namePattern = Pattern.compile("name=.*");
            Pattern descriptionPattern = Pattern.compile("description=.*");
            Pattern publisherPattern = Pattern.compile("publisher=[a-zA-Z0-9_-]{3,16}");
            Pattern versionPattern = Pattern.compile("version=.*");
            Pattern downloadPattern = Pattern.compile("download=[0-9]*");

            files.forEach((directory) -> {
                Path resourceMeta = directory.resolve("resource.meta");
                Path manifest = directory.resolve("manifest.json");

                if (!Files.exists(resourceMeta) || !Files.exists(manifest)) return;
                if (Files.isDirectory(resourceMeta) || Files.isDirectory(manifest)) return;

                JsonInstance element = JsonReader.read(PathUtilities.read(manifest));
                ResourceManifest rm = new ResourceManifest();
                if (rm.read(plugin, element)) {
                    List<String> rawData = PathUtilities.readAllLines(resourceMeta);
                    int id = -1;
                    Category category = null;
                    String name = null;
                    String description = null;
                    String publisher = null;
                    String rsVersion = null;
                    Instant download = null;

                    for (String line : rawData) {
                        Matcher matcher = idPattern.matcher(line);
                        if (matcher.matches()) {
                            id = Integer.parseInt(matcher.group().split("=")[1]);
                        } else if (categoryPattern.matcher(line).matches()) {
                            try {
                                category = Category.valueOf(line.split("=")[1]);
                            } catch (IllegalArgumentException ignored) {}
                        } else if (namePattern.matcher(line).matches()) {
                            name = line.split("=")[1];
                        } else if (descriptionPattern.matcher(line).matches()) {
                            description = line.split("=")[1];
                        } else if (publisherPattern.matcher(line).matches()) {
                            publisher = line.split("=")[1];
                        } else if (versionPattern.matcher(line).matches()) {
                            rsVersion = line.split("=")[1];
                        } else if (downloadPattern.matcher(line).matches()) {
                            download = Instant.ofEpochMilli(Long.parseLong(line.split("=")[1]));
                        }
                    }

                    if (id > 0 && !ObjectUtils.areNullOrEmpty(false, category, name, description,
                            publisher, rsVersion, download)) {
                        CStoredResource resource = CStoredResource.of(
                                id, false, category, name, description, publisher, rsVersion, download, rm
                        );

                        marketPlace.getManager().getResourceSet().add(resource);
                        resource.load();
                    }
                }
            });
        } catch (IOException ex) {
            plugin.log(ex, "Failed to load resources");
        }
    }

    public URI getUpdateURI() {
        URI[] uris = LockLoginJson.getUpdateURIs();
        for (URI uri : uris) {
            try {
                URL url = uri.toURL();
                WebDomain domain = URLUtilities.getDomain(url);
                if (domain == null) continue;

                String host = String.format("%s://%s.%s/locklogin/version", domain.protocol(), domain.root(), domain.tld());
                URL hostURL = URLUtilities.fromString(host);

                if (hostURL == null) continue;

                BuildType type = LockLoginJson.getChannel();
                String flName = type.name().toLowerCase() + ".json";

                return URLUtilities.append(url, flName).toURI();
            } catch (MalformedURLException | URISyntaxException ignored) {}
        }

        return null;
    }
}
