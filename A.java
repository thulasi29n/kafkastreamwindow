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


public class BatchTrackingProcessor implements Processor<String, EnrichedTransaction> {

    private final Duration batchDuration;
    private final int maxBatchSize;
    private final String stateStoreName;

    private ProcessorContext context;
    private KeyValueStore<String, List<EnrichedTransaction>> stateStore;
    private List<EnrichedTransaction> currentBatch;
    private int batchSize;

    public BatchTrackingProcessor(Duration batchDuration, int maxBatchSize) {
        this.batchDuration = batchDuration;
        this.maxBatchSize = maxBatchSize;
        this.stateStoreName = "stateStore";
        this.currentBatch = new ArrayList<>();
        this.batchSize = 0;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.stateStore = (KeyValueStore<String, List<EnrichedTransaction>>) context.getStateStore(stateStoreName);
        
        this.context.schedule(batchDuration, PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            if (!currentBatch.isEmpty()) {
                // Add batch marker and reset the batch size
                currentBatch.add(new EnrichedTransaction(true));
                context.forward(null, currentBatch);
                currentBatch = new ArrayList<>();
                batchSize = 0;
            }
        });
    }

    @Override
    public void process(String key, EnrichedTransaction value) {
        currentBatch.add(value);
        batchSize++;

        // If the batch size has reached the max, forward the batch
        if (batchSize >= maxBatchSize) {
            // Add batch marker and reset the batch size
            currentBatch.add(new EnrichedTransaction(true));
            context.forward(null, currentBatch);
            currentBatch = new ArrayList<>();
            batchSize = 0;
        }
    }

    @Override
    public void close() {
        // Handle the cleanup
    }
}
