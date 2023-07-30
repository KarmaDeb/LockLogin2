package es.karmadev.locklogin.common.api.user.storage.session;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.database.DataDriver;
import es.karmadev.locklogin.api.user.session.SessionField;
import es.karmadev.locklogin.api.user.session.UserSession;

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
    private final DataDriver pool;

    private boolean valid = false;

    private final Map<String, SessionField<?>> fields = new ConcurrentHashMap<>();

    /**
     * Initialize the session
     *
     * @param user_id the client id
     * @param session_id the session id
     * @param pool the session pool
     */
    public CSession(final int user_id, final int session_id, final DataDriver pool) {
        this.id = user_id;
        this.session_id = session_id;
        this.pool = pool;
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
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `captcha_login` FROM `session` WHERE `id` = " + session_id)) {
                if (result.next()) {
                    return result.getBoolean("captcha_login");
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
     * Perform captcha login for this session
     *
     * @param status the login status
     */
    @Override
    public void captchaLogin(final boolean status) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `session` SET `captcha_login` = " + status + " WHERE `id` = " + session_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get if the session is logged
     *
     * @return if the session is logged
     */
    @Override
    public boolean isLogged() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `pass_login` FROM `session` WHERE `id` = " + session_id)) {
                if (result.next()) {
                    return result.getBoolean("pass_login");
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
     * Perform login for this session
     *
     * @param status the login status
     */
    @Override
    public void login(final boolean status) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `session` SET `pass_login` = " + status + " WHERE `id` = " + session_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get if the session is pin logged
     *
     * @return if the session is pin logged
     */
    @Override
    public boolean isPinLogged() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `pin_login` FROM `session` WHERE `id` = " + session_id)) {
                if (result.next()) {
                    return result.getBoolean("pin_login");
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
     * Perform pin login for this session
     *
     * @param status the login status
     */
    @Override
    public void pinLogin(final boolean status) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `session` SET `pin_login` = " + status + " WHERE `id` = " + session_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get if the session is 2fa logged
     *
     * @return if the session is 2fa login
     */
    @Override
    public boolean is2FALogged() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `2fa_login` FROM `session` WHERE `id` = " + session_id)) {
                if (result.next()) {
                    return result.getBoolean("2fa_login");
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
     * Perform 2fa login for this session
     *
     * @param status the login status
     */
    @Override
    public void _2faLogin(final boolean status) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `session` SET `2fa_login` = " + status + " WHERE `id` = " + session_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Set the captcha code
     *
     * @param captcha the captcha code
     */
    @Override
    public void setCaptcha(final String captcha) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `session` SET `captcha` = '" + captcha + "' WHERE `id` = " + session_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Get the session captcha
     *
     * @return the session captcha
     */
    @Override
    public String captcha() {
        String captcha = null;

        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `captcha` FROM `session` WHERE `id` = " + session_id)) {
                if (result.next()) {
                    captcha = result.getString("captcha");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }

        return captcha;
    }

    /**
     * Get if the session is persistent
     *
     * @return if the session is persistent
     */
    @Override
    public boolean isPersistent() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `persistence` FROM `session` WHERE `id` = " + session_id)) {
                if (result.next()) {
                    return result.getBoolean("persistence");
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
     * Set the session persistence
     *
     * @param status persistence status
     */
    @Override
    public void persistent(final boolean status) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            statement.executeUpdate("UPDATE `session` SET `persistence` = " + status + " WHERE `id` = " + session_id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            pool.close(connection, statement);
        }
    }

    /**
     * Append a field to this session
     *
     * @param value the session field to append
     */
    @Override
    public void append(final SessionField<?> value) {
        fields.put(value.key(), value);
    }

    /**
     * Get a field from the session
     *
     * @param key the field key
     * @return the field or null if none
     */
    @Override
    public <T> SessionField<T> fetch(final String key) {
        return (SessionField<T>) fields.getOrDefault(key, null);
    }

    /**
     * Get when the session was created
     *
     * @return the session creation date
     */
    @Override
    public Instant creation() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = pool.retrieve();
            statement = connection.createStatement();
            try (ResultSet result = statement.executeQuery("SELECT `created_at` FROM `session` WHERE `id` = " + session_id)) {
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
