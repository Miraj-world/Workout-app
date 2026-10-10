package com.cs533.workoutlog;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * One background thread for database work. Jobs run in submission order, so a
 * sample-data insert and a Progress refresh never overlap. Results are delivered
 * on the UI thread unless the task was cancelled first.
 */
final class Worker {
    interface Job<T> {
        T run(Task task) throws Exception;
    }

    interface Done<T> {
        void done(T result, Exception error);
    }

    static final class Task {
        private volatile boolean cancelled;
        private Future<?> future;

        boolean isCancelled() {
            return cancelled || Thread.currentThread().isInterrupted();
        }

        void cancel() {
            cancelled = true;
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    /** Call from the UI thread. */
    <T> Task submit(Job<T> job, Done<T> done) {
        Task task = new Task();
        task.future = executor.submit(() -> {
            T result = null;
            Exception error = null;
            try {
                result = job.run(task);
            } catch (Exception e) {
                error = e;
            }
            if (task.isCancelled()) {
                return;
            }
            T finalResult = result;
            Exception finalError = error;
            main.post(() -> {
                if (!task.cancelled) {
                    done.done(finalResult, finalError);
                }
            });
        });
        return task;
    }

    /**
     * Stops queued work and waits briefly for the running job, which checks
     * {@link Task#isCancelled()}, so the caller can close the database safely.
     */
    void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        main.removeCallbacksAndMessages(null);
    }
}
