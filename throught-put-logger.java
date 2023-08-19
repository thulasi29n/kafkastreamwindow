package your.package.name;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import java.time.Duration;

public class ThroughputLogger<K, V> extends AbstractProcessor<K, V> {
    private long startTime;
    private long recordCount = 0;
    private final Duration logInterval;

    public ThroughputLogger(Duration logInterval) {
        this.logInterval = logInterval;
    }

    @Override
    public void init(ProcessorContext context) {
        this.startTime = System.currentTimeMillis();
        context.schedule(logInterval, PunctuationType.WALL_CLOCK_TIME, timestamp -> logThroughput());
        super.init(context);
    }

    @Override
    public void process(K key, V value) {
        recordCount++;
        context().forward(key, value);
    }

    private void logThroughput() {
        long endTime = System.currentTimeMillis();
        double throughput = 1000.0 * recordCount / (endTime - startTime);
        System.out.println("Throughput: " + throughput + " records/second");
        startTime = System.currentTimeMillis();
        recordCount = 0;
    }
}
