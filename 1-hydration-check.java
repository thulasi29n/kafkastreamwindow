@Component
public class StreamProcessor {

    @Autowired
    private StreamsBuilderFactoryBean streamsBuilder;

    private AtomicBoolean isHydrated = new AtomicBoolean(false); // To track hydration status

    public void runStream() {
        StreamsBuilder builder = streamsBuilder.get();
        
        // State hydration for topic1
        KTable<String, Value1> topic1KTable = builder.table("topic1", "stateStoreNameForTopic1");

        // Periodic hydration check using a punctuator
        topic1KTable.toStream().process(() -> new HydrationCheckProcessor(isHydrated));

        // Consume topic2, but only once the state is hydrated
        KStream<String, Value2> topic2Stream = builder.stream("topic2")
            .filter((key, value) -> isHydrated.get());  // Use the hydration status here

        // Joining and branching logic as before
        KStream<String, EnrichedValue> joinedStream = topic2Stream.leftJoin(
            topic1KTable,
            (value2, value1) -> {
                if (value1 != null) {
                    return value2.enrich(value1);
                } else {
                    return null;
                }
            }
        );

        joinedStream.branch(
            (key, value) -> value != null,
            (key, value) -> value == null
        ).forEach(
            matchedStream -> matchedStream.to("matched-topic"),
            unmatchedStream -> unmatchedStream.to("unmatched-topic")
        );

        new KafkaStreams(builder.build(), streamsProperties).start();
    }
}


class HydrationCheckProcessor implements Processor<String, Value1> {

    private final AtomicBoolean isHydrated;
    private ProcessorContext context;

    public HydrationCheckProcessor(AtomicBoolean isHydrated) {
        this.isHydrated = isHydrated;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        // Schedule the punctuator to check hydration status, e.g., every 1 minute
        context.schedule(Duration.ofMinutes(1), PunctuationType.WALL_CLOCK_TIME, this::checkHydrationStatus);
    }

    private void checkHydrationStatus(long timestamp) {
        // Implement your logic to check hydration status
        // Update the isHydrated flag accordingly
        if (/*hydration is complete*/) {
            isHydrated.set(true);
        }
    }

    @Override
    public void process(String key, Value1 value) {
        // No-op for this example, but you can add processing logic if needed
    }

    @Override
    public void close() {}

}


StreamSplitter<String, EnrichedValue> splitter = joinedStream.split();

KStream<String, EnrichedValue>[] branches = splitter
    .branch((key, value) -> value != null)
    .branch((key, value) -> value == null);

KStream<String, EnrichedValue> matchedStream = branches[0];
KStream<String, EnrichedValue> unmatchedStream = branches[1];

matchedStream.to("matched-topic");
unmatchedStream.to("unmatched-topic");


@Component
public class Initializer {

    @Autowired
    private StreamsBuilderFactoryBean streamsBuilderBean;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Access the StreamsBuilder
        StreamsBuilder builder = streamsBuilderBean.getObject();

        // Use the Kafka Consumer API or any other logic here
        // For example, if you need certain properties or settings from your Streams setup:
        Map<String, Object> kafkaProps = streamsBuilderBean.getStreamsConfiguration();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps)) {
            // Your consumer logic here
        }

        // If you have any other initialization logic specific to the StreamsBuilder
        // you can place it here
    }
}


public void populateInitialOffsets() {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
        
        try (KafkaProducer<String, OffsetInfo> producer = new KafkaProducer<>(producerProperties)) {
            for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
                OffsetInfo offsetInfo = new OffsetInfo(0, entry.getValue()); // lastProcessedOffset is 0 initially
                producer.send(new ProducerRecord<>("hydration-tracking-topic", entry.getKey().partition(), offsetInfo));
            }
        }
    }
}



public boolean isHydrationComplete() {
    ReadOnlyKeyValueStore<Integer, OffsetInfo> hydrationStore = 
        streamsBuilder.getKafkaStreams().store("hydration-tracking-store", QueryableStoreTypes.keyValueStore());

    for (int partition = 0; partition < totalPartitions; partition++) {
        OffsetInfo offsetInfo = hydrationStore.get(partition);
        if (offsetInfo.highestKnownOffset - offsetInfo.lastProcessedOffset > acceptableLag) {
            return false;
        }
    }
    return true;
}
class HydrationCheckProcessor implements Processor<String, Value1> {

    private final AtomicBoolean isHydrated;
    private ProcessorContext context;

    public HydrationCheckProcessor(AtomicBoolean isHydrated) {
        this.isHydrated = isHydrated;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        // Schedule the punctuator to check hydration status, e.g., every 1 minute
        context.schedule(Duration.ofMinutes(1), PunctuationType.WALL_CLOCK_TIME, this::checkHydrationStatus);
    }

    private void checkHydrationStatus(long timestamp) {
        // Implement your logic to check hydration status
        // Update the isHydrated flag accordingly
        if (/*hydration is complete*/) {
            isHydrated.set(true);
        }
    }

    @Override
    public void process(String key, Value1 value) {
        // No-op for this example, but you can add processing logic if needed
    }

    @Override
    public void close() {}

}
public class OffsetInfo {
    private long offset;
    private int partition;

    // Constructors, getters, setters, etc.
}
@Component
public class HydrationInitializer {

    @Autowired
    private StreamsBuilderFactoryBean streamsBuilderBean;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        StreamsBuilder builder = streamsBuilderBean.getObject();
        Map<String, Object> kafkaProps = streamsBuilderBean.getStreamsConfiguration();

        // Adjust these configurations for the consumer
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps)) {
            List<PartitionInfo> partitions = consumer.partitionsFor("topic1");

            try (Producer<String, OffsetInfo> producer = new KafkaProducer<>(kafkaProps)) {
                for (PartitionInfo partition : partitions) {
                    int partitionNumber = partition.partition();
                    OffsetInfo initialOffset = new OffsetInfo(0, partitionNumber);
                    producer.send(new ProducerRecord<>("hydration-tracking-topic", String.valueOf(partitionNumber), initialOffset));
                }
            }
        }

        // Your streams processing logic
        // For example: setup your Kafka Streams topology here
    }
}
