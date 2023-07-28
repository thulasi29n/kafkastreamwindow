@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"validatedtransaction", "outputTopic_REGULATOR_1"})
public class TransactionTopologyTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaStreams kafkaStreams;

    private Consumer<String, EnrichedTransaction> consumer;
    private Producer<String, ValidatedTransaction> producer;

    @BeforeEach
    public void setup() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        consumer = new DefaultKafkaConsumerFactory<>(consumerProps).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "outputTopic_REGULATOR_1");

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        producer = new DefaultKafkaProducerFactory<>(producerProps).createProducer();
    }

    @AfterEach
    public void cleanup() {
        consumer.close();
        producer.close();
    }

    @Test
    public void testTransactionEnrichment() {
        // Send a ValidatedTransaction to the input topic
        ValidatedTransaction input = new ValidatedTransaction();
        // Set properties of input...
        producer.send(new ProducerRecord<>("validatedtransaction", input));
        producer.flush();

        // Consume from the output topic
        ConsumerRecord<String, EnrichedTransaction> outputRecord = KafkaTestUtils.getSingleRecord(consumer, "outputTopic_REGULATOR_1");
        EnrichedTransaction output = outputRecord.value();

        // Verify that the output is as expected
        // Assertions.assertEquals(expectedEnrichedTransaction, output);
    }
}
