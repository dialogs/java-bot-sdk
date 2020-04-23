package im.dlg.botsdk.retry;

import java.util.concurrent.CompletableFuture;

public class RetriableTask<R> {

    private final CompletableFuture<R> taskFuture;
    private final int maxRetries;
    private final int minDelay;
    private final int maxDelay;
    private final double delayFactor;

    public RetriableTask(CompletableFuture<R> taskFuture, RetryOptions retryOptions) {
        this.taskFuture = taskFuture;
        this.maxRetries = retryOptions.getMaxRetries();
        this.minDelay = retryOptions.getMinDelay();
        this.maxDelay = retryOptions.getMaxDelay();
        this.delayFactor = retryOptions.getDelayFactor();
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getMinDelay() {
        return minDelay;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public double getDelayFactor() {
        return delayFactor;
    }

    public CompletableFuture<R> getTaskFuture() {
        return taskFuture;
    }

}
