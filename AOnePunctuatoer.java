public class BatchTrackingProcessor implements Processor<String, EnrichedTransaction> {
    private ProcessorContext context;
    private int recordCount = 0;
    private Cancellable scheduledPunctuator;
    private final int countSize;
    private final LocalTime triggerTime;

    public BatchTrackingProcessor(int countSize, LocalTime triggerTime) {
        this.countSize = countSize;
        this.triggerTime = triggerTime;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        context.forward("endOfBatch", null);
        scheduleDailyPunctuator();
    }

    @Override
    public void process(String key, EnrichedTransaction value) {
        context.forward(key, value);
        recordCount++;
        if (recordCount >= countSize) {
            // If so, send an endOfBatch and reset count
            context.forward("endOfBatch", null);
            recordCount = 0;
        }
    }

    private void scheduleDailyPunctuator() {
        long currentTime = System.currentTimeMillis();
        long triggerTimeMillis = triggerTime.atDate(LocalDate.now())
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        long delay = triggerTimeMillis - currentTime;
        if (delay < 0) {
            delay += Duration.ofDays(1).toMillis();
        }
        scheduledPunctuator = context.schedule(Duration.ofMillis(delay), PunctuationType.WALL_CLOCK_TIME, this::punctuateDaily);
    }

    private void punctuateDaily(long timestamp) {
        context.forward("endOfBatch", null);
        scheduledPunctuator.cancel(); // Cancel the old punctuator
        scheduleDailyPunctuator(); // Start a new punctuator
    }

    @Override
    public void close() {
        if (scheduledPunctuator != null) {
            scheduledPunctuator.cancel();
        }
    }
}
