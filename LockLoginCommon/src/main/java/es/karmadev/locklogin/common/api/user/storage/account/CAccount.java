package es.karmadev.locklogin.common.api.user.storage.account;

import es.karmadev.api.core.ExceptionCollector;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.user.account.AccountField;
import es.karmadev.locklogin.api.user.account.UserAccount;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class CAccount implements UserAccount {

    private final int id;
    private final int account_id;
    private final DataDriver pool;

    /**
     * Initialize the account
     *
     * @param user_id the client id
     * @param account_id the account id
     * @param pool the account pool
     */
    public CAccount(final int user_id, final int account_id, final DataDriver pool) {
        this.id = user_id;
        this.account_id = account_id;
        this.pool = pool;
    }

    public void writeHashField(final AccountField field, final HashResult result) {
        String target = null;
        String table = null;
        int use_id = -1;
        if (field.equals(AccountField.PASSWORD) || field.equals(AccountField.PIN) || field.equals(AccountField.PANIC)) {
            table = "user";
            use_id = account_id;

            switch (field) {
                case PASSWORD:
                    target = "password";
                    break;
                case PIN:
                    target = "pin";
                    break;
                case PANIC:
                    target = "panic";
                    break;
            }
        }

        String hash_value = StringUtils.serialize(result);

        if (!ObjectUtils.areNullOrEmpty(false, field, table) && use_id > -1) {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = pool.retrieve();
                statement = connection.createStatement();

                statement.executeUpdate("UPDATE `" + table + "` SET `" + target + "` = '" + hash_value + "' WHERE `id` = " + use_id);
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                pool.close(connection, statement);
            }
        }
    }

    public void writeBooleanValue(final AccountField field, final boolean value) {
        String target = null;
        String table = null;
        int use_id = -1;
        if (field.equals(AccountField.USERNAME) || field.equals(AccountField.UNIQUEID)) {
            table = "user";
            use_id = account_id;

            switch (field) {
                case USERNAME:
                    target = "name";
                    break;
                case UNIQUEID:
                    target = "uuid";
                    break;
            }
        }
        if (field.equals(AccountField.TOKEN_2FA)) {
            table = "account";
            target = "2fa_token";
        }

        if (!ObjectUtils.areNullOrEmpty(false, field, table) && use_id > -1) {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = pool.retrieve();
                statement = connection.createStatement();

                statement.executeUpdate("UPDATE `" + table + "` SET `" + target + "` = " + value + " WHERE `id` = " + use_id);
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                pool.close(connection, statement);
            }
        }
    }

    public void writeStringValue(final AccountField field, final String value) {
        String target = null;
        String table = null;
        int use_id = -1;
        if (field.equals(AccountField.USERNAME) || field.equals(AccountField.UNIQUEID)) {
            table = "user";
            use_id = account_id;

            switch (field) {
                case USERNAME:
                    target = "name";
                    break;
                case UNIQUEID:
                    target = "uuid";
                    break;
            }
        }
        if (field.equals(AccountField.TOKEN_2FA)) {
            table = "account";
            target = "2fa_token";
        }

        if (!ObjectUtils.areNullOrEmpty(false, field, table) && use_id > -1) {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = pool.retrieve();
                statement = connection.createStatement();

                statement.executeUpdate("UPDATE `" + table + "` SET `" + target + "` = '" + value + "' WHERE `id` = " + use_id);
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                pool.close(connection, statement);
            }
        }
    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    @Override
    public int id() {
        return account_id;
    }

    /**
     * Get the account owner id
     *
     * @return the account owner id
     */
    @Override
    public int ownerId() {
        return id;
    }

    /**
     * Removes an account
     *
     * @param issuer the issuer of the account removal
     * @return if the account was able to be removed
     * @throws SecurityException if the caller of this method
     *                           is not a module or plugin
     */
    @Override
    public boolean destroy(final NetworkEntity issuer) throws SecurityException {
        CurrentPlugin.getPlugin().getRuntime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

        return true;
    }

    /**
     * Get the account name
     *
     * @return the account name
     */
    @Override
    public String name() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `name` FROM `user` WHERE `id` = " + id)) {
                if (result.next()) {
                    return result.getString("name");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return null;
    }

    /**
     * Update the account name
     *
     * @param name the account name
     */
    @Override
    public void updateName(final String name) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `user` SET `name` = '" + name + "' WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the account email address
     *
     * @return the account email address
     */
    @Override
    public String email() {
        return null;
    }

    /**
     * Update the account email address
     *
     * @param email the account email address
     */
    @Override
    public void updateEmail(final String email) {

    }

    /**
     * Get the account unique ID
     *
     * @return the account unique ID
     */
    @Override
    public UUID uniqueId() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `uuid` FROM `user` WHERE `id` = " + id)) {
                if (result.next()) {
                    return UUID.fromString(result.getString("uuid"));
                }
            }
        } catch (SQLException | IllegalArgumentException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return null;
    }

    /**
     * Update the account unique id
     *
     * @param uuid the account unique id
     */
    @Override
    public void updateUniqueId(final UUID uuid) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `user` SET `uuid` = '" + uuid.toString() + "' WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the account password
     *
     * @return the account password
     */
    @Override
    public HashResult password() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `password` FROM `account` WHERE `id` = " + account_id)) {
                if (result.next()) {
                    String password = result.getString("password");
                    return StringUtils.loadAndCast(password);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return null;
    }

    /**
     * Set the client password
     *
     * @param password the password
     */
    @Override
    public void setPassword(final String password) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();
        Configuration configuration = plugin.configuration();

        PluginHash hash = hasher.getMethod(configuration.encryption().algorithm());
        HashResult rs = hash.hash(password);
        String result = StringUtils.serialize(rs);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `account` SET `password` = '" + result + "' WHERE `id` = " + account_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    @Override
    public HashResult pin() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `pin` FROM `account` WHERE `id` = " + account_id)) {
                if (result.next()) {
                    String pin = result.getString("pin");
                    return StringUtils.loadAndCast(pin);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return null;
    }

    /**
     * Set the client pin
     *
     * @param pin the pin
     */
    @Override
    public void setPin(final String pin) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();
        Configuration configuration = plugin.configuration();

        PluginHash hash = hasher.getMethod(configuration.encryption().algorithm());
        HashResult rs = hash.hash(pin);
        String result = StringUtils.serialize(rs);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `account` SET `pin` = '" + result + "' WHERE `id` = " + account_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the account 2fa token
     *
     * @return the account 2fa token
     */
    @Override
    public String _2FA() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `2fa_token` FROM `account` WHERE `id` = " + account_id)) {
                if (result.next()) {
                    return result.getString("2fa_token");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return null;
    }

    /**
     * Set the client 2fa token
     *
     * @param token the token
     */
    @Override
    public void set2FA(final String token) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `account` SET `2fa_token` = '" + token + "' WHERE `id` = " + account_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    @Override
    public HashResult panic() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `panic` FROM `account` WHERE `id` = " + account_id)) {
                if (result.next()) {
                    String panic = result.getString("panic");
                    return StringUtils.loadAndCast(panic);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return null;
    }

    /**
     * Set the client panic token
     *
     * @param token the token
     */
    @Override
    public void setPanic(final String token) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();
        Configuration configuration = plugin.configuration();

        PluginHash hash = hasher.getMethod(configuration.encryption().algorithm());
        HashResult rs = hash.hash(token);
        String result = StringUtils.serialize(rs);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `account` SET `panic` = '" + result + "' WHERE `id` = " + account_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Set the client 2fa status
     *
     * @param status the client 2fa status
     */
    @Override
    public void set2FA(final boolean status) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `account` SET `2fa` = " + status + " WHERE `id` = " + account_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get if the account has 2fa enabled
     *
     * @return if the account has 2fa
     */
    @Override
    public boolean has2FA() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `2fa` FROM `account` WHERE `id` = " + account_id)) {
                if (result.next()) {
                    return result.getBoolean("2fa");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return false;
    }

    /**
     * Get when the account was created
     *
     * @return the account creation date
     */
    @Override
    public Instant creation() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `created_at` FROM `account` WHERE `id` = " + account_id)) {
                if (result.next()) {
                    long millis = result.getLong("created_at");
                    return Instant.ofEpochMilli(millis);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return Instant.now();
    }
}
