package io.github.masdaster.motion_tracker;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Z-Byte on .
 */
public class BackgroundWorker {
    private final Set<OnStateChangeListener> onStateChangeListeners = new HashSet<>();
    @NonNull
    private final Runnable<BackgroundWorker> runnable;
    private Thread worker;
    private volatile boolean running;

    public BackgroundWorker(@NonNull Runnable<BackgroundWorker> runnable) {
        this.runnable = runnable;
    }

    public synchronized void start() {
        if (!running) {
            running = true;
            worker = new Thread(() -> runnable.run(this));
            worker.start();
            onStateChange();
        }
    }

    public synchronized void stop() {
        if (running) {
            running = false;
            if (worker != null) {
                try {
                    worker.join();
                } catch (InterruptedException ignored) {
                }
            }
            onStateChange();
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void onStateChange() {
        for (OnStateChangeListener listener : onStateChangeListeners) {
            listener.onStateChange();
        }
    }

    public void addOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        onStateChangeListeners.add(onStateChangeListener);
    }

    public void removeOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        onStateChangeListeners.remove(onStateChangeListener);
    }

    public interface OnStateChangeListener {
        void onStateChange();
    }

    public interface Runnable<T> {
        /**
         * @noinspection unused
         */
        void run(T param);
    }
}
