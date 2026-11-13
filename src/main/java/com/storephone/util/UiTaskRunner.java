package com.storephone.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

public final class UiTaskRunner {
    private UiTaskRunner() {
    }

    public static <T> void runAsync(
            Callable<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError
    ) {
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return backgroundTask.call();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (onError != null) {
                        onError.accept(cause);
                    }
                }
            }
        };
        worker.execute();
    }
}
