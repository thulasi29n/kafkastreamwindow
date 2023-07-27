public class BatchTrackingProcessor implements Processor<String, EnrichedTransaction> {

    private final Duration duration;
    private final long size;
    private ProcessorContext context;
    private List<EnrichedTransaction> transactions = new ArrayList<>();
    private long batchCount = 0;

    public BatchTrackingProcessor(Duration duration, long size) {
        this.duration = duration;
        this.size = size;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        context.schedule(duration, PunctuationType.WALL_CLOCK_TIME, this::punctuate);
    }

    @Override
    public void process(String key, EnrichedTransaction value) {
        transactions.add(value);
        batchCount++;

        if (batchCount >= size) {
            forwardBatch();
        }
    }

    private void forwardBatch() {
        // Forward all messages in the batch to downstream processors
        transactions.forEach(transaction -> context.forward(null, transaction));

        // Clear the transactions list for the next batch
        transactions.clear();
        batchCount = 0;

        // Send an "end of batch" marker
        context.forward("endOfBatch", true);
    }

    private void punctuate(long timestamp) {
        forwardBatch();
    }

    @Override
    public void close() {
        // Make sure to forward the last batch of messages when the processor is being closed
        forwardBatch();
    }
}
