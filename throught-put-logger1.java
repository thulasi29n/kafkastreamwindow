import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class ThroughputLogger {
    private final AtomicLong currentCount = new AtomicLong(0);
    private final AtomicInteger cumulativeCount = new AtomicInteger(0);
    private Instant lastLoggedTime = Instant.now();

    private final Duration loggingInterval;

    public ThroughputLogger(Duration loggingInterval) {
        this.loggingInterval = loggingInterval;
    }

    public void log() {
        long count = currentCount.getAndSet(0);
        cumulativeCount.addAndGet((int) count);

        Instant currentTime = Instant.now();
        Duration elapsed = Duration.between(lastLoggedTime, currentTime);

        if (elapsed.compareTo(loggingInterval) >= 0) {
            double throughput = cumulativeCount.get() / elapsed.getSeconds();

            System.out.println("Throughput: " + throughput + " records/second");

            lastLoggedTime = currentTime;
            cumulativeCount.set(0);
        }
    }

    public void increment() {
        currentCount.incrementAndGet();
    }
}
