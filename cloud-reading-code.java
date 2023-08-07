import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        // Create the consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(getConsumerProps());
        consumer.subscribe(Collections.singletonList("instrument"));

        // Create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(getProducerProps());

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                // Send the record to the Confluent Cloud topic
                producer.send(new ProducerRecord<>("cloud-instrument", record.key(), record.value()));
            }
        }
    }

    private static Properties getConsumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");  // replace with your local Kafka broker address
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "instrument-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    private static Properties getProducerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "your-bootstrap-server-url");  // replace with your Confluent Cloud bootstrap server address
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Security configurations for Confluent Cloud
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username='<API_KEY>' password='<API_SECRET>';");
        return props;
    }
}
