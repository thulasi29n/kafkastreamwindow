import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoadTestAndProcessing {

    private final KafkaProducer<String, String> producer;
    private final String sourceTopic;
    private final String sinkTopic;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LoadTestAndProcessing(String bootstrapServers, String sourceTopic, String sinkTopic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<>(props);
        this.sourceTopic = sourceTopic;
        this.sinkTopic = sinkTopic;
    }

    public void startTestAndProcessing(int runId) {
        switch (runId) {
            case 1:
                scheduleLoadPhase(0, 24, 63);
                // other load phases...
                break;
            // other cases...
            default:
                throw new IllegalArgumentException("Invalid run ID");
        }
        
        // start processing data with Kafka Streams
        startProcessing();
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

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
    
    public static void main(String[] args) {
        LoadTestAndProcessing app = new LoadTestAndProcessing("localhost:9092", "source-topic", "sink-topic");
        app.startTestAndProcessing(1); // start the first run of the test
    }
}
