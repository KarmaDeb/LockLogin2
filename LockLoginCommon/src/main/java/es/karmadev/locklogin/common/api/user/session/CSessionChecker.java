package es.karmadev.locklogin.common.api.user.session;

import es.karmadev.api.schedule.runner.TaskRunner;
import es.karmadev.api.schedule.runner.async.AsyncTaskExecutor;
import es.karmadev.api.schedule.runner.event.TaskEvent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionField;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.api.user.session.check.SessionChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CSessionChecker implements SessionChecker {

    private final LockLogin plugin = CurrentPlugin.getPlugin();
    private final List<Consumer<Boolean>> endListeners = new ArrayList<>();
    private final NetworkClient client;

    private TaskRunner<Long> runner;
    private boolean running = false;
    private boolean cancelled = false;
    private boolean paused = false;

    private boolean resultSet = false;
    private boolean result = false;

    private int authTime;

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
     * Get if the session checker is
     * paused
     *
     * @return if the checker is paused
     */
    @Override
    public boolean isPaused() {
        return paused;
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

            runner.forceTimeLeft((long) authTime);
            runner.resume();
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
     * Pause the session check
     */
    @Override
    public void pause() {
        if (paused) return;
        paused = true;
    }

    /**
     * Resume the session check
     */
    @Override
    public void resume() {
        if (!paused) return;
        paused = false;
    }

    /**
     * Add an end listener
     *
     * @param status the session end listener
     */
    @Override
    public void onEnd(final Consumer<Boolean> status) {
        if (resultSet) {
            status.accept(result);
            return;
        }

        endListeners.add(status);
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
            resultSet = false;
            running = true;
            Configuration configuration = plugin.configuration();
            Messages messages = plugin.messages();
            UserAccount account = client.account();
            UserSession session = client.session();
            AtomicBoolean registered = new AtomicBoolean(account.isRegistered());
            authTime = (registered.get() ? configuration.login().timeout() : configuration.register().timeout());

            AtomicInteger nextLoginMessage = new AtomicInteger(-1);
            AtomicInteger nextRegisterMessage = new AtomicInteger(-1);

            AtomicInteger nextLoginMessage = new AtomicInteger(-1);
            AtomicInteger nextRegisterMessage = new AtomicInteger(-1);

            runner = new AsyncTaskExecutor(authTime, TimeUnit.SECONDS);
            runner.on(TaskEvent.TICK, (time) -> {
                long timeLeft = runner.timeLeft(TimeUnit.SECONDS);

                SessionField<Boolean> field = session.fetch("pass_logged");
                if (field != null && field.get()) {
                    runner.stop();
                    return;
                }

                boolean updatedRegistered = account.isRegistered();
                account.reset("password"); //We want live updates on this

                if (!paused) {
                    if (updatedRegistered != registered.get()) {
                        registered.set(updatedRegistered);
                        int updatedAuthTime = (updatedRegistered ? configuration.login().timeout() : configuration.register().timeout());

                        runner.forceMaxTime((long) updatedAuthTime);
                        if (updatedAuthTime > authTime) {
                            runner.forceTimeLeft((long) updatedAuthTime);
                        }
                    }
                } else {
                    timeLeft += 1;
                    runner.forceTimeLeft(timeLeft); //Do not update
                }

                String captcha = session.captcha();
                if (updatedRegistered) {
                    client.sendTitle(
                            messages.loginTitle(captcha, timeLeft)
                                    .replace('&', '§'),
                            messages.loginSubtitle(captcha, timeLeft)
                                    .replace('&', '§'),
                            0, 20, 0);
                } else {
                    client.sendTitle(
                            messages.registerTitle(captcha, timeLeft)
                                    .replace('&', '§'),
                            messages.registerSubtitle(captcha, timeLeft)
                                    .replace('&', '§'),
                            0, 20, 0);
                }

                if (account.isRegistered()) {
                    if (nextLoginMessage.get() == -1 || nextLoginMessage.get() == time) {
                        client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                        nextLoginMessage.set(time.intValue() + configuration.messageInterval().logging());
                    }
                } else {
                    if (nextRegisterMessage.get() == -1 || nextRegisterMessage.get() == time) {
                        client.sendMessage(messages.prefix() + messages.register(session.captcha()));
                        nextRegisterMessage.set(time.intValue() + configuration.messageInterval().registration());
                    }
                }
            });

            Runnable endTask = () -> {
                if (cancelled) return; //Do nothing

                if (!(boolean) session.fetch("pass_logged").get()) {
                    if (account.isRegistered()) {
                        client.kick(messages.loginTimeOut());
                    } else {
                        client.kick(messages.registerTimeOut());
                    }

                    result = false;
                } else {
                    result = true;
                }

                resultSet = true;
                for (Consumer<Boolean> endListener : endListeners) {
                    endListener.accept(result);
                }
                endListeners.clear();
            };

            runner.on(TaskEvent.END, endTask);
            runner.on(TaskEvent.STOP, endTask);

            runner.start();
        }
    }
}
