package es.karmadev.locklogin.common.api.user.storage.session;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.database.driver.engine.SQLDriver;
import es.karmadev.locklogin.api.plugin.database.query.QueryBuilder;
import es.karmadev.locklogin.api.plugin.database.schema.Row;
import es.karmadev.locklogin.api.plugin.database.schema.Table;
import es.karmadev.locklogin.api.user.session.SessionField;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.plugin.CacheElement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CSession implements UserSession {

    private final int id;
    private final int session_id;
    private final SQLDriver engine;

    private boolean valid = false;

    private boolean valid = false;

    private final Map<String, SessionField<?>> fields = new ConcurrentHashMap<>();

    private final CacheElement<String> captcha = new CacheElement<>();
    private final CacheElement<Boolean> captchaLogged = new CacheElement<>();
    private final CacheElement<Boolean> passwordLogged = new CacheElement<>();
    private final CacheElement<Boolean> totpLogged = new CacheElement<>();
    private final CacheElement<Boolean> pinLogged = new CacheElement<>();
    private final CacheElement<Boolean> persistent = new CacheElement<>();
    private final CacheElement<Instant> creation = new CacheElement<>();

    /**
     * Initialize the session
     *
     * @param user_id the client id
     * @param session_id the session id
     * @param engine the session pool
     */
    public CSession(final int user_id, final int session_id, final SQLDriver engine) {
        this.id = user_id;
        this.session_id = session_id;
        this.engine = engine;
    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    @Override
    public int id() {
        return session_id;
    }

    /**
     * Get the client this session
     * pertains to
     *
     * @return the session owner
     */
    @Override
    public LocalNetworkClient client() {
        return CurrentPlugin.getPlugin().network().getEntity(id);
    }

    /**
     * Validate this session
     */
    @Override
    public void validate() {
        valid = true;
    }

    /**
     * Invalidate this session
     */
    @Override
    public void invalidate() {
        valid = false;
    }

    /**
     * Get if the session is valid
     *
     * @return if the session is valid
     */
    @Override
    public boolean isValid() {
        return valid;
    }

    /**
     * Get if the session is captcha logged
     *
     * @return if the session is captcha logged
     */
    @Override
    public boolean isCaptchaLogged() {
        return captchaLogged.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.SESSION, Row.LOGIN_CAPTCHA)
                        .where(Row.ID, QueryBuilder.EQUALS, session_id).build())) {
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
     * Perform captcha login for this session
     *
     * @param status the login status
     */
    @Override
    public void captchaLogin(final boolean status) {
        if (captchaLogged.isPresent()) {
            if (captchaLogged.getElement() == status) return; //Do nothing, already our value
        }
        captchaLogged.assign(status);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.SESSION)
                    .set(Row.LOGIN_CAPTCHA, status)
                    .where(Row.ID, QueryBuilder.EQUALS, session_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get if the session is logged
     *
     * @return if the session is logged
     */
    @Override
    public boolean isLogged() {
        return passwordLogged.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.SESSION, Row.LOGIN_PASSWORD)
                        .where(Row.ID, QueryBuilder.EQUALS, session_id).build())) {
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
     * Perform login for this session
     *
     * @param status the login status
     */
    @Override
    public void login(final boolean status) {
        if (passwordLogged.isPresent()) {
            if (passwordLogged.getElement() == status) return;
        }
        passwordLogged.assign(status);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.SESSION)
                    .set(Row.LOGIN_PASSWORD, status)
                    .where(Row.ID, QueryBuilder.EQUALS, session_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get if the session is pin logged
     *
     * @return if the session is pin logged
     */
    @Override
    public boolean isPinLogged() {
        return pinLogged.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.SESSION, Row.LOGIN_PIN)
                        .where(Row.ID, QueryBuilder.EQUALS, session_id).build())) {
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
     * Perform pin login for this session
     *
     * @param status the login status
     */
    @Override
    public void pinLogin(final boolean status) {
        if (pinLogged.isPresent()) {
            if (pinLogged.getElement() == status) return;
        }
        pinLogged.assign(status);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.SESSION)
                    .set(Row.LOGIN_PIN, status)
                    .where(Row.ID, QueryBuilder.EQUALS, session_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get if the session is totp logged
     *
     * @return if the session is totp login
     */
    @Override
    public boolean isTotpLogged() {
        return totpLogged.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.SESSION, Row.LOGIN_TOTP)
                        .where(Row.ID, QueryBuilder.EQUALS, session_id).build())) {
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
     * Perform totp login for this session
     *
     * @param status the login status
     */
    @Override
    public void totpLogin(final boolean status) {
        if (totpLogged.isPresent()) {
            if (totpLogged.getElement() == status) return;
        }
        totpLogged.assign(status);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.SESSION)
                    .set(Row.LOGIN_TOTP, status)
                    .where(Row.ID, QueryBuilder.EQUALS, session_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Set the captcha code
     *
     * @param captcha the captcha code
     */
    @Override
    public void setCaptcha(final String captcha) {
        if (this.captcha.isPresent()) {
            if (this.captcha.getElement().equals(captcha)) return;
        }
        this.captcha.assign(captcha);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.SESSION)
                    .set(Row.CAPTCHA, captcha)
                    .where(Row.ID, QueryBuilder.EQUALS, session_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Get the session captcha
     *
     * @return the session captcha
     */
    @Override
    public String captcha() {
        return captcha.getOrElse(() -> {
            String captcha = null;

            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.SESSION, Row.CAPTCHA)
                        .where(Row.ID, QueryBuilder.EQUALS, session_id).build())) {
                    if (result.next()) {
                        captcha = result.getString(1);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                engine.close(connection, statement);
            }

            return captcha;
        });
    }

    /**
     * Get if the session is persistent
     *
     * @return if the session is persistent
     */
    @Override
    public boolean isPersistent() {
        return persistent.getOrElse(() -> {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = engine.retrieve();
                statement = connection.createStatement();
                try (ResultSet result = statement.executeQuery(QueryBuilder.createQuery()
                        .select(Table.SESSION, Row.PERSISTENT)
                        .where(Row.ID, QueryBuilder.EQUALS, session_id).build())) {
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
     * Set the session persistence
     *
     * @param status persistence status
     */
    @Override
    public void persistent(final boolean status) {
        if (persistent.isPresent()) {
            if (persistent.getElement() == status) return;
        }
        persistent.assign(status);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = engine.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate(QueryBuilder.createQuery()
                    .update(Table.SESSION)
                    .set(Row.PERSISTENT, status)
                    .where(Row.ID, QueryBuilder.EQUALS, session_id).build());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            engine.close(connection, statement);
        }
    }

    /**
     * Append a field to this session
     *
     * @param value the session field to append
     */
    @Override
    public void append(final SessionField<?> value) {
        if (value.get() == null) {
            fields.remove(value.key());
        } else {
            fields.put(value.key(), value);
        }
    }

    /**
     * Get a field from the session
     *
     * @param key the field key
     * @return the field or null if none
     */
    @Override @SuppressWarnings("unchecked")
    public <T> SessionField<T> fetch(final String key) {
        try {
            return (SessionField<T>) fields.get(key);
        } catch (ClassCastException ignored) {}
        return null;
    }

    /**
     * Get a field from the session
     *
     * @param key          the field key
     * @param defaultValue the default value if null/none
     * @return the field
     */
    @Override @SuppressWarnings("unchecked")
    public <T> T fetch(final String key, final T defaultValue) {
        if (fields.containsKey(key)) {
            try {
                SessionField<T> field = (SessionField<T>) fields.get(key);
                if (field != null && field.get() != null) return field.get();
            } catch (ClassCastException ignored) {}
        }

        return defaultValue;
    }

    /**
     * Get when the session was created
     *
     * @return the session creation date
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
                        .select(Table.SESSION, Row.CREATED_AT)
                        .where(Row.ID, QueryBuilder.EQUALS, session_id).build())) {
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
            captcha.assign(null);
            captchaLogged.assign(null);
            passwordLogged.assign(null);
            pinLogged.assign(null);
            totpLogged.assign(null);
            persistent.assign(null);
            creation.assign(null);

            return;
        }

        switch (name) {
            case "captcha":
                captcha.assign(null);
                break;
            case "captcha_logged":
                captchaLogged.assign(null);
                break;
            case "pass_logged":
                passwordLogged.assign(null);
                break;
            case "pin_logged":
                pinLogged.assign(null);
                break;
            case "totp_logged":
                totpLogged.assign(null);
                break;
            case "persistent":
                persistent.assign(null);
                break;
            case "creation":
                creation.assign(null);
                break;
            default:
                reset(null);
                break;
        }
    }

    @Override
    public String toString() {
        return "CSession{" +
                "id=" + id +
                ", session_id=" + session_id +
                ", engine=" + engine +
                ", valid=" + valid +
                ", captchaLogged=" + captchaLogged.getElement() +
                ", passwordLogged=" + passwordLogged.getElement() +
                ", totpLogged=" + totpLogged.getElement() +
                ", pinLogged=" + pinLogged.getElement() +
                ", persistent=" + persistent.getElement() +
                ", creation=" + creation.getElement() +
                '}';
    }
}
