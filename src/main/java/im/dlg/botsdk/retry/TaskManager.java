package im.dlg.botsdk.retry;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.function.Function;

public class TaskManager<R> {

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1,
                    new ThreadFactoryBuilder()
                            .setDaemon(true)
                            .setNameFormat("failAfter-%d")
                            .build());

    private final Logger log = LoggerFactory.getLogger(TaskManager.class);
    private final HashSet<Integer> retryCodes = new HashSet<>(Arrays.asList(1, 2, 4, 10, 13, 14, 15));
    private final RetriableTask<R> task;
    private final CompletableFuture<R> future = new CompletableFuture<>();

    public TaskManager(CompletableFuture<R> taskFuture, RetryOptions retryOptions) {
        this.task = new RetriableTask<>(taskFuture, retryOptions);
    }

    public CompletableFuture<R> scheduleTask(int retries) {
        long duration = 0;

        if (retries > 0) {
            duration = Math.min(Math.round(task.getMinDelay() * Math.pow(task.getDelayFactor(), retries - 1)),
                    task.getMaxDelay());
        }

        scheduler.schedule(() -> {
            task.getTaskFuture().applyToEither(new CompletableFuture<>(), Function.identity())
                    .thenAccept(future::complete)
                    .exceptionally(error -> {
                        int code = ((StatusRuntimeException) error.getCause()).getStatus().getCode().value();
                        if (!(retryCodes.contains(code))) {
                            future.completeExceptionally(error);
                            log.error("Failed request to server: ", error);
                            return null;
                        }

                        if (retries > task.getMaxRetries()) {
                            future.completeExceptionally(error);
                            log.error("Failed max retries request to server: ", error);
                        } else {
                            if (retries < task.getMaxRetries()) log.error("Failed request to server: ", error);
                            scheduleTask(retries + 1);
                        }
                        return null;
                    });
        }, duration, TimeUnit.SECONDS);

        return future;
    }
}
