import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoadTestAndProcessing {

    private final KafkaProducer<String, String> producer;
    private final String sourceTopic;
    private final String sinkTopic;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private KafkaStreams streams;

    public LoadTestAndProcessing(String bootstrapServers, String sourceTopic, String sinkTopic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<>(props);
        this.sourceTopic = sourceTopic;
        this.sinkTopic = sinkTopic;
    }

    public void startTestAndProcessing() {
        // Define your load phases
        int totalDuration = 120; // total duration in minutes
        int initialLoad = 63; // initial load (25% of peak)

        List<LoadPhase> loadPhases = new ArrayList<>();
        int phaseDuration = totalDuration / 5; // duration of each phase in minutes

        // Define the loads for each phase based on the initial load.
        int[] loadValues = {
                initialLoad, // Warm-up phase: 25%
                initialLoad * 2, // First Load phase: 50%
                initialLoad * 3, // Sustained Load phase: 80%
                initialLoad * 5, // Second Load phase: 125%
                initialLoad * 2 // Cool-down phase: 50%
        };

        for (int i = 0; i < loadValues.length; i++) {
            int startMinutes = i * phaseDuration;
            loadPhases.add(new LoadPhase(startMinutes, loadValues[i]));
        }

        // start processing data with Kafka Streams
        startProcessing();

        // Start load test
        for (LoadPhase phase : loadPhases) {
            scheduleLoadPhase(phase.getStartMinutes(), phaseDuration, phase.getMessagesPerSecond());
        }
    }

    private void scheduleLoadPhase(int startMinutes, int durationMinutes, int messagesPerSecond) {
        long startMillis = TimeUnit.MINUTES.toMillis(startMinutes);
        long endMillis = startMillis + TimeUnit.MINUTES.toMillis(durationMinutes);
        scheduler.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() > endMillis) {
                throw new RuntimeException("Phase completed");
            }
            for (int i = 0; i < messagesPerSecond; i++) {
                producer.send(new ProducerRecord<>(sourceTopic, "key", "value"));
            }
        }, startMillis, 1000, TimeUnit.MILLISECONDS);
    }

    private void startProcessing() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        // read from the source topic
        KStream<String, String> sourceStream = builder.stream(sourceTopic);

        // write to the sink topic
        sourceStream.to(sinkTopic);

        streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    public static void main(String[] args) {
        LoadTestAndProcessing app = new LoadTestAndProcessing("localhost:9092", "source-topic", "sink-topic");
        app.startTestAndProcessing(); // start the first run of the test
    }
}
