public class HydrationProcessor implements Processor<String, Value> {
    private ProcessorContext context;
    private static final AtomicLong highestOffsetSeen = new AtomicLong(0);
    private static final AtomicBoolean hydrationComplete = new AtomicBoolean(false);
    private final long acceptableLag;

    public HydrationProcessor(long acceptableLag) {
        this.acceptableLag = acceptableLag;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public void process(String key, Value value) {
        long currentOffset = context.offset();
        highestOffsetSeen.set(Math.max(highestOffsetSeen.get(), currentOffset));

        if (value != null && (highestOffsetSeen.get() - currentOffset) <= acceptableLag) {
            hydrationComplete.set(true);
        }
    }

    @Override
    public void close() {}

    public static boolean isHydrationComplete() {
        return hydrationComplete.get();
    }
}


StreamsBuilder builder = new StreamsBuilder();

// Attach your custom processor to topic1 and create the KTable from the resulting stream
KStream<String, Value> hydrationStream = builder.stream("topic1");
hydrationStream.process(() -> new HydrationProcessor(10));  // Process for hydration tracking

KTable<String, Value> stateTable = hydrationStream.toTable();  // Convert the stream to a KTable after processing


// Conditionally process topic2 based on hydration status
KStream<String, OtherValue> topic2Stream = builder.stream("topic2");
topic2Stream
    .filter((key, value) -> HydrationProcessor.isHydrationComplete())
    .join(stateTable, /* join logic */)
    .to("output-topic");  // Send the joined data to an output topic


public class HydrationAwareStreamApp {

    public static void main(String[] args) {
        Properties props = new Properties();
        // ... set up your Kafka Streams properties

        StreamsBuilder builder = new StreamsBuilder();

        // Consume topic1 and populate your KTable or state store
        KTable<String, Value> stateTable = builder.table("topic1", /* configurations */);

        // Attach your custom processor to topic1
        builder.stream("topic1")
               .process(() -> new HydrationProcessor(10)); // 10 is the acceptable lag for example purposes

        // Conditionally process topic2 based on hydration status
        KStream<String, OtherValue> topic2Stream = builder.stream("topic2");
        topic2Stream
            .filter((key, value) -> HydrationProcessor.isHydrationComplete())
            .join(stateTable, /* join logic */)
            .to("output-topic"); // Send the joined data to some output topic

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
