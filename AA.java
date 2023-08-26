import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.kstream.ValueTransformer;
import org.apache.kafka.streams.KeyValue;
import java.time.Duration;

public class TransactionAccumulator implements ValueTransformer<KeyValue<Transaction, Regulator>, KeyValue<String, Transaction>> {

    private ProcessorContext context;
    private long currentCount = 0;
    private long lastPunctuationTime = 0;

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.context.schedule(Duration.ofMillis(5000), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            context.forward("end-of-batch", new Transaction());  // Emitting end-of-batch signal
            currentCount = 0;
            lastPunctuationTime = timestamp;
        });
    }

    @Override
    public KeyValue<String, Transaction> transform(KeyValue<Transaction, Regulator> pair) {
        Transaction transaction = pair.key;
        Regulator regulator = pair.value;

        currentCount++;

        long currentTime = context.timestamp();

        // Checking if it's time to mark end-of-batch either due to count or elapsed time
        if (currentCount >= regulator.getMaxCount() || (currentTime - lastPunctuationTime) >= regulator.getTime()) {
            context.forward("end-of-batch", new Transaction());
            
            // Rescheduling the punctuation based on the regulator's time
            this.context.schedule(Duration.ofMillis(regulator.getTime()), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
                context.forward("end-of-batch", new Transaction());
                currentCount = 0;
                lastPunctuationTime = timestamp;
            });

            lastPunctuationTime = currentTime;
        }

        return new KeyValue<>(transaction.getRegulatorId(), transaction);
    }

    @Override
    public void close() {
        // Cleanup logic if needed
    }
}


KStream<String, KeyValue<Transaction, Regulator>> joinedStream = transactionStream.join(
    regulatorTable,
    (transactionKey, transaction) -> transaction.getRegulatorId(),
    (transaction, regulator) -> new KeyValue<>(transaction, regulator)
);

KStream<String, Transaction> processedStream = joinedStream.transformValues(new TransactionAccumulator());

processedStream.to("output-topic");



