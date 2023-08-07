import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "your-application-id");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        // Specify more properties...

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Instrument> source = builder.stream("instrument-topic");

        source.map((key, instrument) -> {
            // Generate a new key for the transaction
            String transactionKey = UUID.randomUUID().toString();

            Transaction transaction = new Transaction();

            // Generate a random number between 0 and 1
            double rand = new Random().nextDouble();

            // If the random number is less than 0.1, generate a new random ISIN
            if (rand < 0.1) {
                transaction.setInstrumentCode(generateRandomIsin());
            } else {
                transaction.setInstrumentCode(instrument.getIsin());
            }

            // Parse stEndTime as a long and convert it to an Instant
            Instant maxInstant = Instant.ofEpochSecond(Long.parseLong(instrument.getStEndTime()));

            // Generate random epoch between 1990 and stEndTime
            long minEpoch = LocalDate.of(1990, 1, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            long randomEpoch = new Random().longs(minEpoch, maxInstant.getEpochSecond() + 1).findFirst().getAsLong();
            
            // Assign the random epoch as the trade date
            transaction.setTradeDate(String.valueOf(randomEpoch));

            // Generate random data for the other fields in transaction
            generateRandomTransactionData(transaction);

            return new KeyValue<>(transactionKey, transaction);
        }).to("transaction-topic", Produced.with(Serdes.String(), new TransactionSerde()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static String generateRandomIsin() {
        // Replace with your actual logic to generate a random ISIN
        return UUID.randomUUID().toString();
    }

    private static void generateRandomTransactionData(Transaction transaction) {
        // Replace with your actual logic to generate random data for the other fields in Transaction
        // For example:
        transaction.setField1("random value 1");
        transaction.setField2("random value 2");
        // ...
    }
}
