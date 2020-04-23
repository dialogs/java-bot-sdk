package im.dlg.botsdk.retry;

public class RetryOptions {

    public static final RetryOptions DEFAULT_RETRY_OPTIONS = new RetryOptions(5, 1, 50, Math.exp(1));

    private final int maxRetries;
    private final int minDelay;
    private final int maxDelay;
    private final double delayFactor;

    public RetryOptions(int maxRetries, int minDelay, int maxDelay, double delayFactor) {
        this.maxRetries = maxRetries;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.delayFactor = delayFactor;
    }

    public int getMaxRetries() {
        return this.maxRetries;
    }

    public int getMinDelay() {return this.minDelay;}

    public int getMaxDelay() {return this.maxDelay;}

    public double getDelayFactor() {return this.delayFactor;}

    public static final class Builder {

        private int maxRetries = 5;
        private int minDelay  = 1;
        private int maxDelay = 50;
        private double delayFactor = Math.exp(1);

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder withMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        public Builder withMinDelay(int minDelay) {
            this.minDelay = minDelay;
            return this;
        }

        public Builder withMaxDelay(int maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder withDelayFactor(int delayFactor) {
            this.delayFactor = delayFactor;
            return this;
        }

        public RetryOptions build() {
            return new RetryOptions(maxRetries, minDelay, maxDelay, delayFactor);
        }
    }

}
