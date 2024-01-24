package es.karmadev.locklogin.common.api.user.storage.account;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.user.account.AccountField;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.common.api.plugin.CacheElement;
import es.karmadev.locklogin.common.api.protection.CHash;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

public class CAccount implements UserAccount {

    private final int id;
    private final int account_id;
    private final SQLDriver engine;

    private final CacheElement<String> name = new CacheElement<>();
    private final CacheElement<String> email = new CacheElement<>();
    private final CacheElement<UUID> uniqueId = new CacheElement<>();
    private final CacheElement<HashResult> password = new CacheElement<>();
    private final CacheElement<HashResult> pin = new CacheElement<>();
    private final CacheElement<String> totp = new CacheElement<>();
    private final CacheElement<Boolean> totpStatus = new CacheElement<>();
    private final CacheElement<HashResult> panic = new CacheElement<>();
    private final CacheElement<Instant> creation = new CacheElement<>();

    /**
     * Initialize the account
     *
     * @param user_id the client id
     * @param account_id the account id
     * @param engine the account pool
     */
    public CAccount(final int user_id, final int account_id, final SQLDriver engine) {
        this.id = user_id;
        this.account_id = account_id;
        this.engine = engine;
    }

    public void writeHashField(final AccountField field, final HashResult result) {
        if (!field.isType(HashResult.class)) return;

        String hash_value = result.serialize();
        QueryBuilder builder = QueryBuilder.createQuery()
                .update(field.getTable())
                .set(field.getRow(), hash_value)
                .where(Row.ID, QueryBuilder.EQUALS, id);
        
        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(builder.build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    public void writeBooleanValue(final AccountField field, final boolean value) {
        if (!field.isType(Boolean.class)) return;

        QueryBuilder builder = QueryBuilder.createQuery()
                .update(field.getTable())
                .set(field.getRow(), value)
                .where(Row.ID, QueryBuilder.EQUALS, id);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(builder.build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    public void writeStringValue(final AccountField field, final String value) {
        if (!field.isType(String.class)) return;

        QueryBuilder builder = QueryBuilder.createQuery()
                .update(field.getTable())
                .set(field.getRow(), value)
                .where(Row.ID, QueryBuilder.EQUALS, id);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();

            statement.executeUpdate(builder.build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
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

        return true;
    }

    /**
     * Get the account name
     *
     * @return the account name
     */
    @Override
    public String name() {
        return name.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.USER, Row.NAME)
                        .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                    if (result.next()) {
                        return result.getString(1);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return null;
        });
    }

    /**
     * Update the account name
     *
     * @param name the account name
     */
    @Override
    public void updateName(final String name) {
        if (this.name.elementEquals(name)) {
            if (this.name.getElement().equals(name)) return;
        }
        this.name.assign(name);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.NAME, name)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get the account email address
     *
     * @return the account email address
     */
    @Override
    public String email() {
        return email.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.USER, Row.EMAIL)
                        .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                    if (result.next()) {
                        return result.getString(1);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return null;
        });
    }

    /**
     * Update the account email address
     *
     * @param email the account email address
     */
    @Override
    public void updateEmail(final String email) {
        if (this.email.isPresent()) {
            if (this.email.getElement().equals(email)) return;
        }
        this.email.assign(email);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.EMAIL, email)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get the account unique ID
     *
     * @return the account unique ID
     */
    @Override
    public UUID uniqueId() {
        return uniqueId.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.USER, Row.UUID)
                        .where(Row.ID, QueryBuilder.EQUALS, id).build())) {
                    if (result.next()) {
                        return UUID.fromString(result.getString(1));
                    }
                }
            } catch (SQLException | IllegalArgumentException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return null;
        });
    }

    /**
     * Update the account unique id
     *
     * @param uuid the account unique id
     */
    @Override
    public void updateUniqueId(final UUID uuid) {
        if (uniqueId.isPresent()) {
            if (uniqueId.getElement().equals(uuid)) return;
        }
        uniqueId.assign(uuid);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.USER)
                    .set(Row.UUID, uuid)
                    .where(Row.ID, QueryBuilder.EQUALS, id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get the account password
     *
     * @return the account password
     */
    @Override
    public HashResult password() {
        return password.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.ACCOUNT, Row.PASSWORD)
                        .where(Row.ID, QueryBuilder.EQUALS, account_id).build())) {
                    if (result.next()) {
                        String password = result.getString(1);
                        if (!result.wasNull() && !ObjectUtils.isNullOrEmpty(password)) {
                            return CHash.fromString(password, id);
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return null;
        });
    }

    /**
     * Set the client password
     *
     * @param password the password
     */
    @Override
    public void setPassword(final String password) {
        if (this.password.isPresent()) {
            if (this.password.getElement().verify(password)) return;
        }

        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();
        Configuration configuration = plugin.configuration();

        PluginHash hash = hasher.getMethod(configuration.encryption().algorithm());
        HashResult rs = hash.hash(password);
        this.password.assign(rs);

        String result = rs.serialize();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.ACCOUNT)
                    .set(Row.PASSWORD, result)
                    .where(Row.ID, QueryBuilder.EQUALS, account_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    @Override
    public HashResult pin() {
        return pin.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.ACCOUNT, Row.PIN)
                        .where(Row.ID, QueryBuilder.EQUALS, account_id).build())) {
                    if (result.next()) {
                        String pin = result.getString(1);
                        if (!result.wasNull() && !ObjectUtils.isNullOrEmpty(pin)) {
                            return CHash.fromString(pin, id);
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return null;
        });
    }

    /**
     * Set the client pin
     *
     * @param pin the pin
     */
    @Override
    public void setPin(final String pin) {
        if (this.pin.isPresent()) {
            if (this.pin.getElement().verify(pin)) return;
        }

        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();
        Configuration configuration = plugin.configuration();

        PluginHash hash = hasher.getMethod(configuration.encryption().algorithm());
        HashResult rs = hash.hash(pin);
        this.pin.assign(rs);

        String result = rs.serialize();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.ACCOUNT)
                    .set(Row.PIN, result)
                    .where(Row.ID, QueryBuilder.EQUALS, account_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get the account totp token
     *
     * @return the account totp token
     */
    @Override
    public String totp() {
        return totp.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.ACCOUNT, Row.TOKEN_TOTP)
                        .where(Row.ID, QueryBuilder.EQUALS, account_id).build())) {
                    if (result.next()) {
                        return result.getString(1);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return null;
        });
    }

    /**
     * Set the client totp token
     *
     * @param token the token
     */
    @Override
    public void setTotp(final String token) {
        if (totp.isPresent()) {
            if (totp.getElement().equals(token)) return;
        }
        totp.assign(token);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.ACCOUNT)
                    .set(Row.TOKEN_TOTP, token)
                    .where(Row.ID, QueryBuilder.EQUALS, account_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    @Override
    public HashResult panic() {
        return panic.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.ACCOUNT, Row.PANIC)
                        .where(Row.ID, QueryBuilder.EQUALS, account_id).build())) {

                    if (result.next()) {
                        String panic = result.getString(1);
                        if (!result.wasNull() && !ObjectUtils.isNullOrEmpty(panic)) {
                            return CHash.fromString(panic, id);
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return null;
        });
    }

    /**
     * Set the client panic token
     *
     * @param token the token
     */
    @Override
    public void setPanic(final String token) {
        if (panic.isPresent()) {
            if (panic.getElement().verify(token)) return;
        }

        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();
        Configuration configuration = plugin.configuration();

        PluginHash hash = hasher.getMethod(configuration.encryption().algorithm());
        HashResult rs = hash.hash(token);
        panic.assign(rs);

        String result = rs.serialize();

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.ACCOUNT)
                    .set(Row.PANIC, result)
                    .where(Row.ID, QueryBuilder.EQUALS, account_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Set the client totp status
     *
     * @param status the client totp status
     */
    @Override
    public void setTotp(final boolean status) {
        if (totpStatus.isPresent()) {
            if (totpStatus.getElement() == status) return;
        }
        totpStatus.assign(status);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.ACCOUNT)
                    .set(Row.STATUS_TOTP, status)
                    .where(Row.ID, QueryBuilder.EQUALS, account_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get if the account has totp enabled
     *
     * @return if the account has totp
     */
    @Override
    public boolean hasTotp() {
        return totpStatus.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.ACCOUNT, Row.STATUS_TOTP)
                        .where(Row.ID, QueryBuilder.EQUALS, account_id).build())) {
                    if (result.next()) {
                        return result.getBoolean(1);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return false;
        });
    }

    /**
     * Get when the account was created
     *
     * @return the account creation date
     */
    @Override
    public Instant creation() {
        return creation.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.ACCOUNT, Row.CREATED_AT)
                        .where(Row.ID, QueryBuilder.EQUALS, account_id).build())) {
                    if (result.next()) {
                        long millis = result.getLong(1);
                        return Instant.ofEpochMilli(millis);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return Instant.now();
        });
    }

    /**
     * Reset the cache, implementations
     * should interpreter null as "everything"
     *
     * @param name the cache name to reset
     */
    @Override
    public void reset(final String name) {
        if (name == null) {
            this.name.assign(null);
            email.assign(null);
            uniqueId.assign(null);
            password.assign(null);
            pin.assign(null);
            totp.assign(null);
            totpStatus.assign(null);
            panic.assign(null);
            creation.assign(null);
            return;
        }

        switch (name) {
            case "name":
                this.name.assign(null);
                break;
            case "email":
                this.email.assign(null);
                break;
            case "uuid":
                this.uniqueId.assign(null);
                break;
            case "password":
                this.password.assign(null);
                break;
            case "pin":
                this.pin.assign(null);
                break;
            case "totp":
                this.totp.assign(null);
                break;
            case "totp_status":
                this.totpStatus.assign(null);
                break;
            case "panic":
                this.panic.assign(null);
                break;
            case "creation":
                creation.assign(null);
                break;
            default:
                reset(null);
                break;
        }
    }
}
