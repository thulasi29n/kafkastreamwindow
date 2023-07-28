@Configuration
public class TransactionTopology {

    @Autowired
    private StreamsBuilder builder;

    @Autowired
    private GlobalKTable<String, ReferenceData> globalKTable;

    @Bean
    public KafkaStreams kafkaStreams() {
        // Assuming Regulator is the enum of regulators
        Regulator[] regulators = Regulator.values();

        KStream<String, ValidatedTransaction> inputStream = builder.stream("validatedtransaction");
        KStream<String, ValidatedTransaction> repartitionedStream = inputStream.selectKey((key, value) -> value.getRegulatorID());

        List<Branched<KStream<String, ValidatedTransaction>>> branches = 
            Stream.of(regulators).map(regulatorId -> 
                Branched.<String, ValidatedTransaction>as(regulatorId.toString())
                    .withFunction(stream -> stream
                        .join(globalKTable,
                            (key, value) -> key,
                            (validatedTransaction, referenceData) -> {
                                // Enrich the ValidatedTransaction with reference data from the globalKTable
                                // You can extract the countSize and triggerTime for each regulator from the reference data
                                int countSize = referenceData.getCountSize();
                                LocalTime triggerTime = referenceData.getTriggerTime();

                                EnrichedTransaction enrichedTransaction = new EnrichedTransaction();
                                // ... Enrichment logic ...
                                enrichedTransaction.setProcessor(new BatchTrackingProcessor(countSize, triggerTime));

                                return enrichedTransaction;
                            })
                        .process(() -> value -> value.getProcessor())
                        .to("outputTopic_" + regulatorId.toString(), Produced.with(Serdes.String(), new EnrichedTransactionSerde())))
            )
            .collect(Collectors.toList());

        Map<String, KStream<String, ValidatedTransaction>> branchedStreams = repartitionedStream
                .split(branches.toArray(new Branched[0]));

        KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfig());
        streams.start();
        return streams;
    }

    private Properties streamsConfig() {
        Properties config = new Properties();
        // Define your streams configuration here...
        return config;
    }
}
