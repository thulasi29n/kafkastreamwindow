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
