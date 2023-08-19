import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValue;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.To;

public class StreamingApp {

    public static void main(String[] args) {
        final StreamsBuilder builder = new StreamsBuilder();

        // Read from input topic
        KStream<String, String> source = builder.stream("input-topic");

        // Transform the input. Assuming the transformation logic to newKey and newValue is available.
        KStream<String, String> transformed = source.map((key, value) -> {
            String newKey = ...; // transform your key here
            String newValue = ...; // transform your value here
            return new KeyValue<>(newKey, newValue);
        });

        // Add a transformer to measure throughput. 
        transformed.transform(() -> new ThroughputLogger<String, String>(5000));  // 5000 ms = 5 seconds

        // Write the results back to a new topic
        transformed.to("output-topic", Produced.with(new CustomPartitioner()));

        KafkaStreams streams = new KafkaStreams(builder.build(), ...); // pass your properties
        streams.start();
    }
}

class ThroughputLogger<K, V> implements Transformer<K, V, KeyValue<K, V>> {
    private ProcessorContext context;
    private long lastTimestamp = -1;
    private long processedSinceLastLog = 0;
    private final long logIntervalMs;

    public ThroughputLogger(long logIntervalMs) {
        this.logIntervalMs = logIntervalMs;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public KeyValue<K, V> transform(K key, V value) {
        if (lastTimestamp == -1) {
            lastTimestamp = context.timestamp();
        }

        processedSinceLastLog++;

        if (context.timestamp() - lastTimestamp > logIntervalMs) {
            System.out.println("Processed " + processedSinceLastLog + " records in the last " + logIntervalMs + " ms.");
            processedSinceLastLog = 0;
            lastTimestamp = context.timestamp();
        }

        return new KeyValue<>(key, value);
    }

    @Override
    public void close() {}
}

// CustomPartitioner class remains as previous.
