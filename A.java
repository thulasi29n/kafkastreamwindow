KStream<String, ValidatedTransaction> inputStream = builder.stream("validatedtransaction");
KStream<String, ValidatedTransaction> repartitionedStream = inputStream.selectKey((key, value) -> value.getRegulatorID());

// Assuming Regulator is the enum of regulators
Regulator[] regulators = Regulator.values();
Branched<KStream<String, ValidatedTransaction>>[] branches = new Branched[regulators.length];

for (int i = 0; i < regulators.length; i++) {
    String regulatorId = regulators[i].toString();
    branches[i] = Branched.<String, ValidatedTransaction>as(regulatorId)
            .withFunction(stream -> stream
                    .join(globalKTable,
                            (key, value) -> key,
                            (validatedTransaction, referenceData) -> {
                                // Enrich the ValidatedTransaction with reference data from the globalKTable
                                EnrichedTransaction enrichedTransaction = new EnrichedTransaction();
                                // ... Enrichment logic ...
                                return enrichedTransaction;
                            })
                    .process(() -> new BatchTrackingProcessor(Duration.ofMinutes(25), 1_000_000))
                    .to("outputTopic_" + regulatorId, Produced.with(Serdes.String(), new EnrichedTransactionSerde())));
}

Map<String, KStream<String, ValidatedTransaction>> branchedStreams = repartitionedStream
        .split(branches);
