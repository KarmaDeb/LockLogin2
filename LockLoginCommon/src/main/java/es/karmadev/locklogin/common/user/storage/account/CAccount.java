package es.karmadev.locklogin.common.user.storage.account;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.common.user.SQLPooling;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.sql.*;
import java.util.UUID;

public class CAccount implements UserAccount {

    private final int id;
    private final SQLPooling pool;

    /**
     * Initialize the account
     *
     * @param id the account id
     * @param pool the account pool
     */
    public CAccount(final int id, final SQLPooling pool) {
        this.id = id;
        this.pool = pool;
    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    @Override
    public int id() {
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
        CurrentPlugin.getPlugin().runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

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
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT `name` FROM `users` WHERE `id` = " + id);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getString("name");
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
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `users` SET `name` = '" + name + "' WHERE `id` = " + id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the account unique ID
     *
     * @return the account unique ID
     */
    @Override
    public UUID uniqueId() {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT `uuid` FROM `users` WHERE `id` = " + id);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return UUID.fromString(result.getString("uuid"));
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
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `users` SET `uuid` = '" + uuid.toString() + "' WHERE `id` = " + id);
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
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT `password` FROM `users` WHERE `id` = " + id);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                String password = result.getString("password");
                return StringUtils.loadUnsafe(password);
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

        PluginHash hash = hasher.getMethod(configuration.encryption().passwordAlgorithm());
        HashResult rs = hash.hash(password);
        String result = StringUtils.serialize(rs);

        Connection connection = null;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `users` SET `password` = '" + result + "' WHERE `id` = " + id);
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
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT `pin` FROM `users` WHERE `id` = " + id);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                String pin = result.getString("pin");
                return StringUtils.loadUnsafe(pin);
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

        PluginHash hash = hasher.getMethod(configuration.encryption().pinAlgorithm());
        HashResult rs = hash.hash(pin);
        String result = StringUtils.serialize(rs);

        Connection connection = null;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `users` SET `pin` = '" + result + "' WHERE `id` = " + id);
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
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT `2fa_token` FROM `users` WHERE `id` = " + id);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getString("2fa_token");
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
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `users` SET `2fa_token` = '" + token + "' WHERE `id` = " + id);
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
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT `panic` FROM `users` WHERE `id` = " + id);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                String panic = result.getString("panic");
                return StringUtils.loadUnsafe(panic);
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

        PluginHash hash = hasher.getMethod(configuration.encryption().passwordAlgorithm());
        HashResult rs = hash.hash(token);
        String result = StringUtils.serialize(rs);

        Connection connection = null;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `users` SET `panic` = '" + result + "' WHERE `id` = " + id);
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
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `users` SET `2fa` = " + status + " WHERE `id` = " + id);
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
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT `2fa` FROM `users` WHERE `id` = " + id);
            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getBoolean("2fa");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return false;
    }
}
