import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.kstream.internals.KTableImpl;

public class AggregationApp {
    public static void main(String[] args) {

        StreamsBuilder builder = new StreamsBuilder();

        // Define the initial KStream
        KStream<String, Regulator> source = builder.stream("input-topic", Consumed.with(Serdes.String(), new JsonSerde<>(Regulator.class)));

        // Define the aggregation logic
        KTable<String, RegulatorAggregate> aggregateTable = source.groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(Regulator.class)))
                .aggregate(
                        // Initializer
                        RegulatorAggregate::new,
                        // Aggregator
                        (key, newValue, aggValue) -> {
                            aggValue.incrementCount();
                            aggValue.setLatestValue(newValue.getMaxInstructionCount());
                            return aggValue;
                        },
                        // Materialize the state store
                        Materialized.<String, RegulatorAggregate, KeyValueStore<Bytes, byte[]>>as("aggregation-store")
                                .withValueSerde(new JsonSerde<>(RegulatorAggregate.class))
                );

        // Write the result back to another topic, if needed
        aggregateTable.toStream().to("output-topic", Produced.with(Serdes.String(), new JsonSerde<>(RegulatorAggregate.class)));

        // Rest of the Kafka Streams application setup and start...
    }
}

// Define the Regulator class
class Regulator {
    private String regulatorId;
    private Integer maxInstructionCount;
    // getters, setters, and other fields...
}

// Define the RegulatorAggregate class
class RegulatorAggregate {
    private int count;
    private Integer latestValue;

    public void incrementCount() {
        this.count++;
    }

    // getters, setters...
}
