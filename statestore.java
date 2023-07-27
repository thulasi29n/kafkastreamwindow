public class BatchTrackingProcessor implements Processor<String, EnrichedTransaction> {

    private final Duration duration;
    private final long size;
    private ProcessorContext context;
    private KeyValueStore<String, EnrichedTransaction> stateStore;
    private long batchCount = 0;

    public BatchTrackingProcessor(Duration duration, long size) {
        this.duration = duration;
        this.size = size;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.stateStore = (KeyValueStore<String, EnrichedTransaction>) context.getStateStore("BatchStateStore");
        context.schedule(duration, PunctuationType.WALL_CLOCK_TIME, this::punctuate);
    }

    @Override
    public void process(String key, EnrichedTransaction value) {
        // Put the transaction into the state store
        stateStore.put(key, value);
        batchCount++;

        if (batchCount >= size) {
            forwardBatch();
        }
    }

    private void forwardBatch() {
        // Forward all messages in the batch to downstream processors
        KeyValueIterator<String, EnrichedTransaction> it = stateStore.all();
        while (it.hasNext()) {
            KeyValue<String, EnrichedTransaction> entry = it.next();
            context.forward(entry.key, entry.value);
        }
        it.close();

        // Clear the state store for the next batch
        stateStore.all().forEachRemaining(kv -> stateStore.delete(kv.key));
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
