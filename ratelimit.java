import com.google.common.util.concurrent.RateLimiter;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoadTestAndProcessing {

    private KafkaStreams streams;
    private RateLimiter rateLimiter;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private void startProcessing(String sourceTopic, String sinkTopic, int... ratesInMps) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        // start with the first rate
        this.rateLimiter = RateLimiter.create(ratesInMps[0]);

        // schedule rate changes
        for (int i = 1; i < ratesInMps.length; i++) {
            int rateInMps = ratesInMps[i];
            scheduler.schedule(() -> rateLimiter.setRate(rateInMps), i * 24, TimeUnit.MINUTES);
        }

        // read from the source topic
        KStream<String, String> sourceStream = builder.stream(sourceTopic);

        // add processing delay
        sourceStream.mapValues((ValueMapper<String, String>) value -> {
            rateLimiter.acquire(); // this will block until the required number of permits are available
            return value;
        }).to(sinkTopic);

        streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    public static void main(String[] args) {
        LoadTestAndProcessing app = new LoadTestAndProcessing();
        app.startProcessing("source-topic", "sink-topic", 63, 125, 200, 313, 125); // start processing with a rate of 63 MPS, then change to 125 MPS after 24 mins, etc.
    }
}
