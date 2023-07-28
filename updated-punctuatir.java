public class BatchTrackingProcessor implements Processor<String, EnrichedTransaction> {

    private ProcessorContext context;
    private int recordThreshold;
    private Cancellable schedule;

    public BatchTrackingProcessor(Duration timeThreshold, int recordThreshold) {
        this.recordThreshold = recordThreshold;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;

        // Schedule the punctuator to run every 25 minutes
        this.schedule = context.schedule(Duration.ofMinutes(25), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            // Send batch end marker
            context.forward("endOfBatch", true);
            // Reset record count
            recordCount = 0;
        });
    }

    @Override
    public void process(String key, EnrichedTransaction value) {
        context.forward(key, value);
        recordCount++;

        if (recordCount >= recordThreshold) {
            // Send batch end marker
            context.forward("endOfBatch", true);
            // Reset record count
            recordCount = 0;
            // Cancel the current punctuator schedule and schedule a new one
            schedule.cancel();
            schedule = context.schedule(Duration.ofMinutes(25), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
                context.forward("endOfBatch", true);
                recordCount = 0;
            });
        }
    }

    @Override
    public void close() {}
}
