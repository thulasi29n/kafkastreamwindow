public class MyTopologyTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, ValidatedTransaction> inputTopic;
    private TestOutputTopic<String, EnrichedTransaction> outputTopic;
    private Serde<String> stringSerde = Serdes.String();
    private Serde<ValidatedTransaction> validatedTransactionSerde = new ValidatedTransactionSerde();
    private Serde<EnrichedTransaction> enrichedTransactionSerde = new EnrichedTransactionSerde();

    @BeforeEach
    public void setup() {
        // setup test driver 
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        Topology topology = new MyTopologyBuilder().build(); // assuming this is your topology class
        testDriver = new TopologyTestDriver(topology, props);

        // setup test topics
        inputTopic = testDriver.createInputTopic("validatedtransaction", stringSerde.serializer(), validatedTransactionSerde.serializer());
        outputTopic = testDriver.createOutputTopic("outputTopic", stringSerde.deserializer(), enrichedTransactionSerde.deserializer());
    }

    @Test
    public void shouldProcessCorrectly() {
        // arrange
        ValidatedTransaction vt1 = new ValidatedTransaction(...); // create test data
        ValidatedTransaction vt2 = new ValidatedTransaction(...); // create test data

        // act
        inputTopic.pipeInput("key1", vt1);
        inputTopic.pipeInput("key2", vt2);

        // assert
        // assuming "outputTopic" is the output topic name of your topology
        // and that `EnrichedTransaction` is the value type on the output topic
        EnrichedTransaction result1 = outputTopic.readKeyValue().value;
        EnrichedTransaction result2 = outputTopic.readKeyValue().value;
        
        // perform your assertions here...
    }

    @AfterEach
    public void tearDown() {
        try {
            testDriver.close();
        } catch (final RuntimeException e) {
            System.err.println("Ignoring exception, test failing due this exception: " + e.getLocalizedMessage());
        }
    }
}


@Test
public void shouldProcessCorrectly() {
    // arrange
    ValidatedTransaction vt1 = new ValidatedTransaction(...); // create test data
    ValidatedTransaction vt2 = new ValidatedTransaction(...); // create test data

    // act
    // Send two messages
    inputTopic.pipeInput("key1", vt1);
    inputTopic.pipeInput("key2", vt2);

    // Advance the wall clock time by 25 minutes
    testDriver.advanceWallClockTime(Duration.ofMinutes(25));

    // Send two more messages
    ValidatedTransaction vt3 = new ValidatedTransaction(...); // create test data
    ValidatedTransaction vt4 = new ValidatedTransaction(...); // create test data
    inputTopic.pipeInput("key3", vt3);
    inputTopic.pipeInput("key4", vt4);

    // assert
    // assuming "outputTopic" is the output topic name of your topology
    // and that `EnrichedTransaction` is the value type on the output topic
    EnrichedTransaction result1 = outputTopic.readKeyValue().value;
    EnrichedTransaction result2 = outputTopic.readKeyValue().value;
    // Check the end of batch marker after 25 minutes
    assertThat(outputTopic.readKeyValue().key).isEqualTo("endOfBatch");

    // perform your assertions here...

    EnrichedTransaction result3 = outputTopic.readKeyValue().value;
    EnrichedTransaction result4 = outputTopic.readKeyValue().value;
    
    // perform your assertions here...
}
