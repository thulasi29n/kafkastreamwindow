public class MyTopologyBuilder {

    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<ValidatedTransaction> validatedTransactionSerde = new ValidatedTransactionSerde();
    private final Serde<EnrichedTransaction> enrichedTransactionSerde = new EnrichedTransactionSerde();

    public Topology build() {
        // Create a StreamsBuilder
        StreamsBuilder builder = new StreamsBuilder();

        // Assuming Regulator is the enum of regulators
        Regulator[] regulators = Regulator.values();

        // Create the initial stream
        KStream<String, ValidatedTransaction> inputStream = builder.stream("validatedtransaction");
        KStream<String, ValidatedTransaction> repartitionedStream = inputStream.selectKey((key, value) -> value.getRegulatorID());

        // Create a GlobalKTable for the reference data
        GlobalKTable<String, ReferenceData> globalKTable = builder.globalTable("referenceDataTopic");

        // Create a stream for each regulator
        Map<String, KStream<String, ValidatedTransaction>> regulatorStreams = new HashMap<>();
        for (Regulator regulator : regulators) {
            regulatorStreams.put(
                regulator.toString(),
                repartitionedStream.filter((key, value) -> key.equals(regulator.toString()))
            );
        }

        // Process each regulator stream
        regulatorStreams.forEach((regulatorId, stream) ->
            stream.join(
                globalKTable,
                (key, value) -> key,
                (validatedTransaction, referenceData) -> {
                    // Extract the countSize and triggerTime for each regulator from the reference data
                    int countSize = referenceData.getCountSize();
                    LocalTime triggerTime = referenceData.getTriggerTime();

                    // Instantiate the BatchTrackingProcessor with the extracted countSize and triggerTime
                    BatchTrackingProcessor batchProcessor = new BatchTrackingProcessor(countSize, triggerTime);

                    // Process each validatedTransaction with the batchProcessor
                    batchProcessor.process(key, validatedTransaction);

                    // Return the validatedTransaction to continue the stream
                    return validatedTransaction;
                }
            )
            .to("outputTopic_" + regulatorId, Produced.with(stringSerde, validatedTransactionSerde))
        );

        // Build and return the topology
        return builder.build();
    }
}
