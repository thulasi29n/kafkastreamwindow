public class IsinPartitioner implements StreamPartitioner<String, String> {
    @Override
    public Integer partition(String topic, String isin, String value, int numPartitions) {
        // numPartitions will be the number of partitions of the target topic, 
        // so in the case of "instrument-new", it should be 48.
        return Math.abs(isin.hashCode()) % numPartitions;
    }
}



import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.StreamPartitioner;
import org.apache.kafka.streams.KeyValue;

public class RepartitionTopology {
    public static void main(String[] args) {
        final StreamsBuilder builder = new StreamsBuilder();

        // Assume a simple JSON parser that extracts isin and timestamp
        builder.stream("instrument-hist", Consumed.with(Serdes.String(), Serdes.String()))
               .map((key, value) -> {
                   String isin = extractIsin(value);  // Extract isin from the value JSON
                   String timestamp = extractTimestamp(value); // Extract timestamp from the value JSON
                   return KeyValue.pair(isin + timestamp, value);
               })
               .to("instrument-new", Produced.with(Serdes.String(), Serdes.String(), new IsinPartitioner()));

        // Further Kafka Streams setup and start code...
    }

    // Placeholder functions for extracting isin and timestamp from your JSON.
    private static String extractIsin(String value) {
        // Your logic to extract isin from the JSON
    }

    private static String extractTimestamp(String value) {
        // Your logic to extract timestamp from the JSON
    }
}
