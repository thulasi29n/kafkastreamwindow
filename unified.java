@SpringBootApplication
public class UnifiedApp {

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    public static void main(String[] args) {
        SpringApplication.run(UnifiedApp.class, args);
    }

    @Bean
    public Properties kafkaProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return props;
    }

    @Bean
    public KafkaStreams kafkaStreams(Properties kafkaProperties) {
        StreamsBuilder builder = new StreamsBuilder();
        // Define your topology here
        builder.table("input-topic", Materialized.as("my-table"));

        KafkaStreams streams = new KafkaStreams(builder.build(), kafkaProperties);
        streams.start();
        return streams;
    }

    @PreDestroy
    public void shutdownStreams() {
        kafkaStreams(kafkaProperties()).close();
    }

}

@RestController
@RequestMapping("/statestore")
public class StateStoreController {

    private final KafkaStreams kafkaStreams;

    @Autowired
    public StateStoreController(KafkaStreams kafkaStreams) {
        this.kafkaStreams = kafkaStreams;
    }

    @GetMapping("/get/{key}")
    public ResponseEntity<String> getValue(@PathVariable String key) {
        try {
            ReadOnlyKeyValueStore<String, String> keyValueStore = 
                kafkaStreams.store(StoreQueryParameters.fromNameAndType("my-table", QueryableStoreTypes.keyValueStore()));

            String value = keyValueStore.get(key);
            if (value != null) {
                return new ResponseEntity<>(value, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Key not found in state store", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


// Assume you have these Serdes
Serde<Regulator> regulatorSerde = ...;  // Obtain or create a Serde for your Regulator class
Serde<String> stringSerde = Serdes.String();

// Use them in the Materialized definition
builder.table("Regulator", Materialized.<String, Regulator, KeyValueStore<Bytes, byte[]>>as("my-table")
        .withKeySerde(stringSerde)
        .withValueSerde(regulatorSerde));


@RestController
class KeyValueStoreController {

    @Autowired
    private KafkaStreams streams;

    @GetMapping("/getKey/{key}")
    public String getKey(@PathVariable String key) {
        ReadOnlyKeyValueStore<String, String> store = streams.store("my-table", QueryableStoreTypes.keyValueStore());
        return store.get(key);
    }
}


StoreQueryParameters<ReadOnlyKeyValueStore<String, String>> storeQueryParameters = 
    StoreQueryParameters.fromNameAndType("my-table", QueryableStoreTypes.keyValueStore());

ReadOnlyKeyValueStore<String, String> store = streams.store(storeQueryParameters);
