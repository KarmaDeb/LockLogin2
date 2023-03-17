package es.karmadev.locklogin.common.api.plugin.service.backup;

import com.google.gson.*;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.security.backup.BackupService;
import es.karmadev.locklogin.api.security.backup.RestoreMethod;
import es.karmadev.locklogin.api.security.backup.store.BackupStorage;
import es.karmadev.locklogin.api.security.backup.store.UserBackup;
import es.karmadev.locklogin.api.security.backup.task.BackupRestoreTask;
import es.karmadev.locklogin.api.security.backup.task.ScheduledBackup;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.AccountField;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.account.migration.AccountMigrator;
import es.karmadev.locklogin.api.user.session.UserSession;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CLocalBackup implements BackupService {

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "Local Backups";
    }

    /**
     * Perform a backup
     *
     * @return the backup task
     */
    @Override
    public ScheduledBackup performBackup() {
        String id = TokenGenerator.generateLiteral();
        return performBackup(id);
    }

    /**
     * Perform a backup
     *
     * @param id the backup id
     * @return the backup task
     */
    @Override
    public ScheduledBackup performBackup(final String id) {
        String name = UUID.randomUUID().toString().replaceAll("-", "");
        CBackupTask task = new CBackupTask(id);

        LockLogin plugin = CurrentPlugin.getPlugin();
        CompletableFuture.runAsync(() -> {
            Consumer<Throwable> error_catcher = task.error;

            try {
                Gson gson = new GsonBuilder().create();
                UserAccount[] accounts = plugin.getAccountFactory(false).getAllAccounts();

                Collection<LocalNetworkClient> clients = plugin.network().getPlayers();

                JsonArray stored_accounts = new JsonArray();
                Runnable start_run = task.start;
                if (start_run != null) {
                    start_run.run();
                }

                List<UserBackup> backups = new ArrayList<>();
                for (UserAccount account : accounts) {
                    error_catcher = task.error; //Just in case it gets redefined
                    try {
                        LocalNetworkClient account_owner = clients.stream().filter((client) -> client.account().id() == account.id()).findFirst().orElse(null);
                        if (account_owner != null) {
                            CBackupUser backup = CBackupUser.from(account, account_owner);
                            stored_accounts.add(StringUtils.serialize(backup));
                            backups.add(backup);
                        }
                    } catch (Throwable unexpected) {
                        error_catcher.accept(unexpected);
                        break;
                    }
                }

                Instant creation = Instant.now();

                JsonObject backup = new JsonObject();
                backup.addProperty("id", id);
                backup.addProperty("stored", stored_accounts.size());
                backup.addProperty("created", creation.toEpochMilli());
                backup.add("accounts", stored_accounts);

                error_catcher = task.error;
                try {
                    Path destination = plugin.workingDirectory().resolve("data").resolve("service").resolve("backups").resolve(name + ".json");
                    PathUtilities.create(destination);

                    String json = gson.toJson(backup);
                    Files.write(destination, json.getBytes(StandardCharsets.UTF_8));

                    Consumer<BackupStorage> success = task.success;
                    if (success != null) {
                        success.accept(CBackupStorage.forAccounts(id, backups, creation, destination));
                    }
                } catch (Throwable unexpected) {
                    error_catcher.accept(unexpected);
                }
            } catch (Throwable unexpected) {
                error_catcher.accept(unexpected);
            }
        });

        return task;
    }

    /**
     * Get all the backups
     *
     * @return all the backups
     */
    @Override
    public BackupStorage[] fetchAll() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path backups_location = plugin.workingDirectory().resolve("data").resolve("service").resolve("backups");

        List<BackupStorage> fetched = new ArrayList<>();
        try(Stream<Path> files = Files.list(backups_location).filter((path) -> !Files.isDirectory(path) && PathUtilities.getExtension(path).equals("json"))) {
            List<Path> backup_files = files.collect(Collectors.toList());

            Gson gson = new GsonBuilder().create();
            for (Path backup : backup_files) {
                try {
                    JsonObject json = gson.fromJson(Files.newBufferedReader(backup), JsonObject.class);

                    String id = json.get("id").getAsString();
                    Instant created = Instant.ofEpochMilli(json.get("created").getAsLong());
                    JsonArray serial_accounts = json.get("accounts").getAsJsonArray();

                    List<UserBackup> backups = new ArrayList<>();
                    for (JsonElement element : serial_accounts) {
                        String serialized = element.getAsString();
                        backups.add(StringUtils.loadUnsafe(serialized));
                    }

                    fetched.add(CBackupStorage.forAccounts(id, backups, created, backup));
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ex) {
            plugin.log(ex, "Failed to fetch backups");
        }

        return fetched.toArray(new BackupStorage[0]);
    }

    /**
     * Get a backup
     *
     * @param id the backup id
     * @return the backup
     */
    @Override
    public BackupStorage fetch(final String id) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path backups_location = plugin.workingDirectory().resolve("data").resolve("service").resolve("backups");

        BackupStorage fetched = null;
        try(Stream<Path> files = Files.list(backups_location).filter((path) -> !Files.isDirectory(path) && PathUtilities.getExtension(path).equals("json"))) {
            List<Path> backup_files = files.collect(Collectors.toList());

            Gson gson = new GsonBuilder().create();
            for (Path backup : backup_files) {
                try {
                    JsonObject json = gson.fromJson(Files.newBufferedReader(backup), JsonObject.class);

                    if (id.equals(json.get("id").getAsString())) {
                        Instant created = Instant.ofEpochMilli(json.get("created").getAsLong());
                        JsonArray serial_accounts = json.get("accounts").getAsJsonArray();

                        List<UserBackup> backups = new ArrayList<>();
                        for (JsonElement element : serial_accounts) {
                            String serialized = element.getAsString();
                            backups.add(StringUtils.loadUnsafe(serialized));
                        }

                        fetched = CBackupStorage.forAccounts(id, backups, created, backup);
                        break;
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ex) {
            plugin.log(ex, "Failed to fetch backup " + id);
        }

        return fetched;
    }

    /**
     * Purge all the backups
     *
     * @return the amount of purged
     * backups
     */
    @Override
    public int purgeAll() {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path backups_location = plugin.workingDirectory().resolve("data").resolve("service").resolve("backups");

        int success = 0;
        try(Stream<Path> files = Files.list(backups_location).filter((path) -> !Files.isDirectory(path) && PathUtilities.getExtension(path).equals("json"))) {
            List<Path> backup_files = files.collect(Collectors.toList());

            for (Path backup : backup_files) {
                if (PathUtilities.destroyWithResults(backup)) {
                    success++;
                }
            }
        } catch (Throwable ex) {
            plugin.log(ex, "Failed to purge all backups");
        }

        return success;
    }

    /**
     * Purge all the backups between the provided
     * dates
     *
     * @param start the start date
     * @param end   the end date
     * @return the amount of purged
     * backups
     */
    @Override
    public int purgeBetween(final Instant start, final Instant end) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path backups_location = plugin.workingDirectory().resolve("data").resolve("service").resolve("backups");

        int purged = 0;
        try(Stream<Path> files = Files.list(backups_location).filter((path) -> !Files.isDirectory(path) && PathUtilities.getExtension(path).equals("json"))) {
            List<Path> backup_files = files.collect(Collectors.toList());

            Gson gson = new GsonBuilder().create();
            for (Path backup : backup_files) {
                try {
                    JsonObject json = gson.fromJson(Files.newBufferedReader(backup), JsonObject.class);

                    Instant created = Instant.ofEpochMilli(json.get("created").getAsLong());
                    if (created.isAfter(start) && created.isBefore(end)) {
                        if (PathUtilities.destroyWithResults(backup)) {
                            purged++;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ex) {
            plugin.log(ex, "Failed to purge backups");
        }

        return purged;
    }

    /**
     * Purge all the backups made after
     * the specified date
     *
     * @param start the start date
     * @return the purged backups
     */
    @Override
    public int purgeSince(final Instant start) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path backups_location = plugin.workingDirectory().resolve("data").resolve("service").resolve("backups");

        int purged = 0;
        try(Stream<Path> files = Files.list(backups_location).filter((path) -> !Files.isDirectory(path) && PathUtilities.getExtension(path).equals("json"))) {
            List<Path> backup_files = files.collect(Collectors.toList());

            Gson gson = new GsonBuilder().create();
            for (Path backup : backup_files) {
                try {
                    JsonObject json = gson.fromJson(Files.newBufferedReader(backup), JsonObject.class);

                    Instant created = Instant.ofEpochMilli(json.get("created").getAsLong());
                    if (created.isAfter(start)) {
                        if (PathUtilities.destroyWithResults(backup)) {
                            purged++;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ex) {
            plugin.log(ex, "Failed to purge backups");
        }

        return purged;
    }

    /**
     * Purge all the backups made before
     * the specified date
     *
     * @param end the end date
     * @return the purged backups
     */
    @Override
    public int purgeUntil(final Instant end) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        Path backups_location = plugin.workingDirectory().resolve("data").resolve("service").resolve("backups");

        int purged = 0;
        try(Stream<Path> files = Files.list(backups_location).filter((path) -> !Files.isDirectory(path) && PathUtilities.getExtension(path).equals("json"))) {
            List<Path> backup_files = files.collect(Collectors.toList());

            Gson gson = new GsonBuilder().create();
            for (Path backup : backup_files) {
                try {
                    JsonObject json = gson.fromJson(Files.newBufferedReader(backup), JsonObject.class);

                    Instant created = Instant.ofEpochMilli(json.get("created").getAsLong());
                    if (created.isBefore(end)) {
                        if (PathUtilities.destroyWithResults(backup)) {
                            purged++;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ex) {
            plugin.log(ex, "Failed to purge backups");
        }

        return purged;
    }

    /**
     * Restore all the backups
     *
     * @param method the restore method
     * @return the restore task
     */
    @Override
    public BackupRestoreTask restoreAll(final RestoreMethod method) {
        CRestoreTask task = new CRestoreTask();

        LockLogin plugin = CurrentPlugin.getPlugin();
        PluginNetwork network = plugin.network();

        AccountFactory<UserAccount> factory = plugin.getAccountFactory(false);
        AccountMigrator<UserAccount> migrator = factory.migrator();

        CompletableFuture.runAsync(() -> {
            BackupStorage[] backups = fetchAll();

            Set<Integer> restored = Collections.newSetFromMap(new ConcurrentHashMap<>());
            int index = 0;
            AtomicBoolean problem = new AtomicBoolean();

            Function<Integer, Void> finish = integer -> {
                if (!problem.get()) {
                    Consumer<Integer> success = task.success;
                    if (success != null) success.accept(restored.size());
                }

                return null;
            };

            for (BackupStorage storage : backups) {
                index++;
                UserBackup[] accounts = storage.accounts();
                BiConsumer<Throwable, Integer> error = task.error;

                for (UserBackup user : accounts) {
                    try {
                        LocalNetworkClient local = network.getEntity(user.account());
                        UserSession session = local.session();
                        UserAccount account = local.account();

                        restored.add(local.id());

                        try {
                            switch (method) {
                                case EVERYTHING:
                                    migrator.migrate(local, user);
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case FIELD_WHITELIST:
                                    migrator.migrate(local, user, method.getFieldsReverse());
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case FIELD_BLACKLIST:
                                    migrator.migrate(local, user, method.getFields());
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case NON_SET:
                                    List<AccountField> fields = new ArrayList<>();
                                    fields.add(AccountField.USERNAME); //Always set
                                    fields.add(AccountField.UNIQUEID); //Always set
                                    fields.add(AccountField.SESSION_PERSISTENCE); //Always set
                                    fields.add(AccountField.STATUS_2FA); //Always set

                                    if (account == null) {
                                        fields.clear();
                                    } else {
                                        if (account.isRegistered()) {
                                            fields.add(AccountField.PASSWORD);
                                        }
                                        if (account.hasPin()) {
                                            fields.add(AccountField.PIN);
                                        }
                                        if (account._2faSet()) {
                                            fields.add(AccountField.TOKEN_2FA);
                                        }
                                        if (account.isProtected()) {
                                            fields.add(AccountField.PANIC);
                                        }
                                    }

                                    migrator.migrate(local, user, fields.toArray(new AccountField[0]));
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                            }
                        } catch (Throwable unexpected) {
                            if (error != null) error.accept(unexpected, restored.size());
                            problem.set(true);
                            break;
                        }
                    } catch (Throwable unexpected) {
                        if (error != null) error.accept(unexpected, restored.size());
                        problem.set(true);

                        break;
                    }

                    if (index == backups.length - 1) {
                        finish.apply(restored.size());
                    }
                }
            }
        });

        return task;
    }

    /**
     * Restore a backup
     *
     * @param backup the backup to restore
     * @param method the restore method
     * @return the restore task
     */
    @Override
    public BackupRestoreTask restore(final BackupStorage backup, final RestoreMethod method) {
        CRestoreTask task = new CRestoreTask();

        LockLogin plugin = CurrentPlugin.getPlugin();
        PluginNetwork network = plugin.network();

        AccountFactory<UserAccount> factory = plugin.getAccountFactory(false);
        AccountMigrator<UserAccount> migrator = factory.migrator();

        CompletableFuture.runAsync(() -> {
            Set<Integer> restored = Collections.newSetFromMap(new ConcurrentHashMap<>());
            UserBackup[] accounts = backup.accounts();
            BiConsumer<Throwable, Integer> error = task.error;

            boolean problem = false;
            for (UserBackup user : accounts) {
                try {
                    LocalNetworkClient local = network.getEntity(user.account());
                    UserSession session = local.session();
                    UserAccount account = local.account();

                    restored.add(local.id());

                    try {
                        switch (method) {
                            case EVERYTHING:
                                migrator.migrate(local, user);
                                if (session != null) {
                                    session.persistent(user.sessionPersistent());
                                }
                                break;
                            case FIELD_WHITELIST:
                                migrator.migrate(local, user, method.getFieldsReverse());
                                if (session != null) {
                                    session.persistent(user.sessionPersistent());
                                }
                                break;
                            case FIELD_BLACKLIST:
                                migrator.migrate(local, user, method.getFields());
                                if (session != null) {
                                    session.persistent(user.sessionPersistent());
                                }
                                break;
                            case NON_SET:
                                List<AccountField> fields = new ArrayList<>();
                                fields.add(AccountField.USERNAME); //Always set
                                fields.add(AccountField.UNIQUEID); //Always set
                                fields.add(AccountField.SESSION_PERSISTENCE); //Always set
                                fields.add(AccountField.STATUS_2FA); //Always set

                                if (account == null) {
                                    fields.clear();
                                } else {
                                    if (account.isRegistered()) {
                                        fields.add(AccountField.PASSWORD);
                                    }
                                    if (account.hasPin()) {
                                        fields.add(AccountField.PIN);
                                    }
                                    if (account._2faSet()) {
                                        fields.add(AccountField.TOKEN_2FA);
                                    }
                                    if (account.isProtected()) {
                                        fields.add(AccountField.PANIC);
                                    }
                                }

                                migrator.migrate(local, user, fields.toArray(new AccountField[0]));
                                if (session != null) {
                                    session.persistent(user.sessionPersistent());
                                }
                                break;
                        }
                    } catch (Throwable unexpected) {
                        if (error != null) error.accept(unexpected, restored.size());
                        problem = true;
                        break;
                    }
                } catch (Throwable unexpected) {
                    if (error != null) error.accept(unexpected, restored.size());
                    problem = true;
                    break;
                }
            }

            if (!problem) {
                Consumer<Integer> success = task.success;
                if (success != null) success.accept(restored.size());
            }
        });

        return task;
    }

    /**
     * Restore all the backups between the provided ones
     *
     * @param from   the backup to start from
     * @param to     the backup to end at
     * @param method the restore method
     * @return the restore task
     */
    @Override
    public BackupRestoreTask restore(final BackupStorage from, final BackupStorage to, final RestoreMethod method) {
        CRestoreTask task = new CRestoreTask();

        LockLogin plugin = CurrentPlugin.getPlugin();
        PluginNetwork network = plugin.network();

        AccountFactory<UserAccount> factory = plugin.getAccountFactory(false);
        AccountMigrator<UserAccount> migrator = factory.migrator();

        Instant start = from.creation();
        Instant end = to.creation();

        String startId = from.id();
        String endId = to.id();
        CompletableFuture.runAsync(() -> {
            BackupStorage[] backups = fetchAll();

            Set<Integer> restored = Collections.newSetFromMap(new ConcurrentHashMap<>());

            boolean problem = false;
            Set<UserBackup[]> suitable_accounts = new LinkedHashSet<>();
            for (BackupStorage storage : backups) {
                String id = storage.id();
                Instant time = storage.creation();

                if (id.equals(startId) || id.equals(endId) || time.isAfter(start) || time.isBefore(end)) {
                    UserBackup[] accounts = storage.accounts();
                    suitable_accounts.add(accounts);
                }
            }

            for (UserBackup[] accounts : suitable_accounts) {
                BiConsumer<Throwable, Integer> error = task.error;

                for (UserBackup user : accounts) {
                    try {
                        LocalNetworkClient local = network.getEntity(user.account());
                        UserSession session = local.session();
                        UserAccount account = local.account();

                        restored.add(local.id());

                        try {
                            switch (method) {
                                case EVERYTHING:
                                    migrator.migrate(local, user);
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case FIELD_WHITELIST:
                                    migrator.migrate(local, user, method.getFieldsReverse());
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case FIELD_BLACKLIST:
                                    migrator.migrate(local, user, method.getFields());
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case NON_SET:
                                    List<AccountField> fields = new ArrayList<>();
                                    fields.add(AccountField.USERNAME); //Always set
                                    fields.add(AccountField.UNIQUEID); //Always set
                                    fields.add(AccountField.SESSION_PERSISTENCE); //Always set
                                    fields.add(AccountField.STATUS_2FA); //Always set

                                    if (account == null) {
                                        fields.clear();
                                    } else {
                                        if (account.isRegistered()) {
                                            fields.add(AccountField.PASSWORD);
                                        }
                                        if (account.hasPin()) {
                                            fields.add(AccountField.PIN);
                                        }
                                        if (account._2faSet()) {
                                            fields.add(AccountField.TOKEN_2FA);
                                        }
                                        if (account.isProtected()) {
                                            fields.add(AccountField.PANIC);
                                        }
                                    }

                                    migrator.migrate(local, user, fields.toArray(new AccountField[0]));
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                            }
                        } catch (Throwable unexpected) {
                            if (error != null) error.accept(unexpected, restored.size());
                            problem = true;
                            break;
                        }
                    } catch (Throwable unexpected) {
                        if (error != null) error.accept(unexpected, restored.size());
                        problem = true;
                        break;
                    }
                }
            }

            if (!problem) {
                Consumer<Integer> success = task.success;
                success.accept(restored.size());
            }
        });

        return task;
    }

    /**
     * Restore all the backups made since the
     * specified backup
     *
     * @param from  the start backup
     * @param method the restore method
     * @return the restore task
     */
    @Override
    public BackupRestoreTask restoreSince(final BackupStorage from, final RestoreMethod method) {
        CRestoreTask task = new CRestoreTask();

        LockLogin plugin = CurrentPlugin.getPlugin();
        PluginNetwork network = plugin.network();

        AccountFactory<UserAccount> factory = plugin.getAccountFactory(false);
        AccountMigrator<UserAccount> migrator = factory.migrator();

        Instant start = from.creation();

        String startId = from.id();
        CompletableFuture.runAsync(() -> {
            BackupStorage[] backups = fetchAll();

            Set<Integer> restored = Collections.newSetFromMap(new ConcurrentHashMap<>());

            boolean problem = false;
            Set<UserBackup[]> suitable_accounts = new LinkedHashSet<>();
            for (BackupStorage storage : backups) {
                String id = storage.id();
                Instant time = storage.creation();

                if (id.equals(startId) || time.isAfter(start)) {
                    UserBackup[] accounts = storage.accounts();
                    suitable_accounts.add(accounts);
                }
            }

            for (UserBackup[] accounts : suitable_accounts) {
                BiConsumer<Throwable, Integer> error = task.error;

                for (UserBackup user : accounts) {
                    try {
                        LocalNetworkClient local = network.getEntity(user.account());
                        UserSession session = local.session();
                        UserAccount account = local.account();

                        restored.add(local.id());

                        try {
                            switch (method) {
                                case EVERYTHING:
                                    migrator.migrate(local, user);
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case FIELD_WHITELIST:
                                    migrator.migrate(local, user, method.getFieldsReverse());
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case FIELD_BLACKLIST:
                                    migrator.migrate(local, user, method.getFields());
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case NON_SET:
                                    List<AccountField> fields = new ArrayList<>();
                                    fields.add(AccountField.USERNAME); //Always set
                                    fields.add(AccountField.UNIQUEID); //Always set
                                    fields.add(AccountField.SESSION_PERSISTENCE); //Always set
                                    fields.add(AccountField.STATUS_2FA); //Always set

                                    if (account == null) {
                                        fields.clear();
                                    } else {
                                        if (account.isRegistered()) {
                                            fields.add(AccountField.PASSWORD);
                                        }
                                        if (account.hasPin()) {
                                            fields.add(AccountField.PIN);
                                        }
                                        if (account._2faSet()) {
                                            fields.add(AccountField.TOKEN_2FA);
                                        }
                                        if (account.isProtected()) {
                                            fields.add(AccountField.PANIC);
                                        }
                                    }

                                    migrator.migrate(local, user, fields.toArray(new AccountField[0]));
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                            }
                        } catch (Throwable unexpected) {
                            if (error != null) error.accept(unexpected, restored.size());
                            problem = true;
                            break;
                        }
                    } catch (Throwable unexpected) {
                        if (error != null) error.accept(unexpected, restored.size());
                        problem = true;
                        break;
                    }
                }
            }

            if (!problem) {
                Consumer<Integer> success = task.success;
                success.accept(restored.size());
            }
        });

        return task;
    }

    /**
     * Restore all the backups made until the
     * specified backup
     *
     * @param to    the end backup
     * @param method the restore method
     * @return the restore task
     */
    @Override
    public BackupRestoreTask restoreUntil(final BackupStorage to, final RestoreMethod method) {
        CRestoreTask task = new CRestoreTask();

        LockLogin plugin = CurrentPlugin.getPlugin();
        PluginNetwork network = plugin.network();

        AccountFactory<UserAccount> factory = plugin.getAccountFactory(false);
        AccountMigrator<UserAccount> migrator = factory.migrator();

        Instant end = to.creation();

        String endId = to.id();
        CompletableFuture.runAsync(() -> {
            BackupStorage[] backups = fetchAll();

            Set<Integer> restored = Collections.newSetFromMap(new ConcurrentHashMap<>());

            boolean problem = false;
            Set<UserBackup[]> suitable_accounts = new LinkedHashSet<>();
            for (BackupStorage storage : backups) {
                String id = storage.id();
                Instant time = storage.creation();

                if (id.equals(endId) || time.isBefore(end)) {
                    UserBackup[] accounts = storage.accounts();
                    suitable_accounts.add(accounts);
                }
            }

            for (UserBackup[] accounts : suitable_accounts) {
                BiConsumer<Throwable, Integer> error = task.error;

                for (UserBackup user : accounts) {
                    try {
                        LocalNetworkClient local = network.getEntity(user.account());
                        UserSession session = local.session();
                        UserAccount account = local.account();

                        restored.add(local.id());

                        try {
                            switch (method) {
                                case EVERYTHING:
                                    migrator.migrate(local, user);
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case FIELD_WHITELIST:
                                    migrator.migrate(local, user, method.getFieldsReverse());
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case FIELD_BLACKLIST:
                                    migrator.migrate(local, user, method.getFields());
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                                case NON_SET:
                                    List<AccountField> fields = new ArrayList<>();
                                    fields.add(AccountField.USERNAME); //Always set
                                    fields.add(AccountField.UNIQUEID); //Always set
                                    fields.add(AccountField.SESSION_PERSISTENCE); //Always set
                                    fields.add(AccountField.STATUS_2FA); //Always set

                                    if (account == null) {
                                        fields.clear();
                                    } else {
                                        if (account.isRegistered()) {
                                            fields.add(AccountField.PASSWORD);
                                        }
                                        if (account.hasPin()) {
                                            fields.add(AccountField.PIN);
                                        }
                                        if (account._2faSet()) {
                                            fields.add(AccountField.TOKEN_2FA);
                                        }
                                        if (account.isProtected()) {
                                            fields.add(AccountField.PANIC);
                                        }
                                    }

                                    migrator.migrate(local, user, fields.toArray(new AccountField[0]));
                                    if (session != null) {
                                        session.persistent(user.sessionPersistent());
                                    }
                                    break;
                            }
                        } catch (Throwable unexpected) {
                            if (error != null) error.accept(unexpected, restored.size());
                            problem = true;
                            break;
                        }
                    } catch (Throwable unexpected) {
                        if (error != null) error.accept(unexpected, restored.size());
                        problem = true;
                        break;
                    }
                }
            }

            if (!problem) {
                Consumer<Integer> success = task.success;
                success.accept(restored.size());
            }
        });

        return task;
    }
}
