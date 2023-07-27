public class BatchTrackingProcessor implements Processor<String, EnrichedTransaction> {

    private final Duration batchDuration;
    private final int maxBatchSize;

    private ProcessorContext context;
    private List<EnrichedTransaction> currentBatch;
    private int batchSize;

    public BatchTrackingProcessor(Duration batchDuration, int maxBatchSize) {
        this.batchDuration = batchDuration;
        this.maxBatchSize = maxBatchSize;
        this.currentBatch = new ArrayList<>();
        this.batchSize = 0;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        
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
