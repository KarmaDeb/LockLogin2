package es.karmadev.locklogin.common.api.user.session;

import es.karmadev.api.core.source.APISource;
import es.karmadev.api.schedule.runner.TaskRunner;
import es.karmadev.api.schedule.runner.async.AsyncTaskExecutor;
import es.karmadev.api.schedule.runner.event.TaskEvent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.api.user.session.check.SessionChecker;
import es.karmadev.locklogin.common.api.user.storage.session.CSession;

import java.util.concurrent.TimeUnit;

public class CSessionChecker implements SessionChecker {

    private final LockLogin plugin = CurrentPlugin.getPlugin();
    private final NetworkClient client;

    private TaskRunner runner;
    private boolean running = false;
    private boolean cancelled = false;

    /**
     * Initialize the session checker
     *
     * @param client the client
     */
    public CSessionChecker(final NetworkClient client) {
        this.client = client;
    }

    /**
     * Get if the session checker is
     * running
     *
     * @return if the checker is running
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Restart the session check
     */
    @Override
    public void restart() {
        if (!running) return;
        if (runner != null) {
            cancelled = true;
            runner.stop();
            running = false;
            cancelled = false;

            runner.start();
            running = true;
        }
    }

    /**
     * Cancel the session check
     */
    @Override
    public void cancel() {
        if (!running) return;
        if (runner != null) {
            cancelled = true;
            runner.stop();
            running = false;
        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (!running) {
            running = true;
            Configuration configuration = plugin.configuration();
            Messages messages = plugin.messages();
            UserAccount account = client.account();
            UserSession session = client.session();
            boolean registered = account.isRegistered();

            int authTime = (registered ? configuration.login().timeout() : configuration.register().timeout());
            runner = new AsyncTaskExecutor(authTime, TimeUnit.SECONDS);
            runner.on(TaskEvent.TICK, (time) -> {
                long timeLeft = runner.timeLeft(TimeUnit.SECONDS);

                if (session == null) {
                    runner.stop();
                    return;
                }

                if (session.isLogged()) {
                    //client.sendMessage(messages.prefix() + messages.logged());
                    runner.stop();
                    return;
                }

                String captcha = session.captcha();
                if (account.isRegistered()) {
                    client.sendTitle(messages.loginTitle(captcha, timeLeft), messages.loginSubtitle(captcha, timeLeft), 0, 3, 0);
                } else {
                    client.sendTitle(messages.registerTitle(captcha, timeLeft), messages.registerSubtitle(captcha, timeLeft), 0, 3, 0);
                }
            });
            Runnable endTask = () -> {
                if (cancelled) return; //Do nothing

                if (!session.isLogged()) {
                    if (account.isRegistered()) {
                        client.kick(messages.loginTimeOut());
                    } else {
                        client.kick(messages.registerTimeOut());
                    }
                }
            };

            runner.on(TaskEvent.END, endTask);
            runner.on(TaskEvent.STOP, endTask);

            runner.start();
        }
    }
}
