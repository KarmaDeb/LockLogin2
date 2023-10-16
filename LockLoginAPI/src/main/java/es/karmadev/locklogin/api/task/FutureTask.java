package es.karmadev.locklogin.api.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a task that has been
 * executed
 */
public class FutureTask<T> implements Future<T> {

    @Nullable
    private T result;
    @Nullable
    private Throwable error;

    private boolean running = false;
    private boolean cancelled = false;

    @Nullable
    private BiTask<T, Throwable> completionListener;

    /**
     * Complete the task
     *
     * @param result the task result
     */
    public final void complete(final T result) {
        complete(result, null);
    }

    /**
     * Complete the task
     *
     * @param error the task error
     */
    public final void complete(final Throwable error) {
        complete(null, error);
    }

    /**
     * Complete the task asynchronously
     *
     * @param supplier the task result supplier
     */
    public void completeAsynchronously(final Supplier<T> supplier) {
        CompletableFuture.runAsync(() -> {
            try {
                T value = supplier.get();
                complete(value);
            } catch (Throwable ex) {
                complete(null, ex);
            }
        });
    }

    /**
     * Complete the task
     *
     * @param result the task result if any
     * @param error the error if any
     */
    public final void complete(final T result, final Throwable error) {
        if (this.result != null || this.error != null || cancelled) return;

        this.result = result;
        this.error = error;

        running = true;
        CompletableFuture.runAsync(() -> {
           while (running) {
               if (cancelled) {
                   if (this.completionListener != null) {
                       this.completionListener.throwException(new CancellationException());
                   }

                   running = false;
               }
           }
        });

        if (this.completionListener != null) {
            this.completionListener.accept(result, error);
        }
        running = false;
    }

    /**
     * Executes the specified action when
     * the task gets completed (or if the
     * task has been already completed)
     *
     * @param completion the completion task
     */
    public void whenComplete(final Runnable completion) {
        whenComplete((a, b) -> completion.run());
    }

    /**
     * Executes the specified action when
     * the task gets completed (or if the
     * task has been already completed)
     *
     * @param completion the completion task
     */
    public void whenComplete(final Consumer<T> completion) {
        whenComplete((a, b) -> completion.accept(a));
    }

    /**
     * Executes the specified action when
     * the task gets completed (or if the
     * task has been already completed)
     *
     * @param completion the completion task
     */
    public void whenComplete(final BiTask<T, Throwable> completion) {
        if (cancelled) return; //Prevent from even setting a value if cancelled

        this.completionListener = completion;
        if (result != null) {
            this.completionListener.accept(result, error);
        }
    }

    /**
     * Attempts to cancel execution of this task.  This method has no
     * effect if the task is already completed or cancelled, or could
     * not be cancelled for some other reason.  Otherwise, if this
     * task has not started when {@code cancel} is called, this task
     * should never run.  If the task has already started, then the
     * {@code mayInterruptIfRunning} parameter determines whether the
     * thread executing this task (when known by the implementation)
     * is interrupted in an attempt to stop the task.
     *
     * <p>The return value from this method does not necessarily
     * indicate whether the task is now cancelled; use {@link
     * #isCancelled}.
     *
     * @param mayInterruptIfRunning {@code true} if the thread
     *                              executing this task should be interrupted (if the thread is
     *                              known to the implementation); otherwise, in-progress tasks are
     *                              allowed to complete
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed; {@code true}
     * otherwise. If two or more threads cause a task to be cancelled,
     * then at least one of them returns {@code true}. Implementations
     * may provide stronger guarantees.
     */
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        if (running && mayInterruptIfRunning) {
            cancelled = true;
        }

        return cancelled;
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Returns {@code true} if this task completed.
     * <p>
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    @Override
    public boolean isDone() {
        return result != null || error != null;
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (result != null) return result;

        Thread thread = Thread.currentThread();
        synchronized (thread) {
            while (result == null) {
                if (cancelled) {
                    throw new CancellationException();
                }

                thread.wait(1);
            }
        }

        return result;
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     * @throws TimeoutException      if the wait timed out
     */
    @Override
    public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, TimeoutException {
        if (result != null) return result;

        long wait = TimeUnit.MILLISECONDS.convert(timeout, unit);
        Thread thread = Thread.currentThread();
        synchronized (thread) {
            while (result == null || wait-- > 0) {
                if (cancelled) {
                    throw new CancellationException();
                }

                thread.wait(1);
            }
        }
        if (wait <= 0) throw new TimeoutException();

        return result;
    }
}
