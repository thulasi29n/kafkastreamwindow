// Define the Serde for the tracking info
final Serde<OffsetInfo> offsetInfoSerde = Serdes.serdeFrom(new OffsetInfoSerializer(), new OffsetInfoDeserializer());

// Create the GlobalKTable for the hydration-tracking-topic
GlobalKTable<String, OffsetInfo> hydrationTrackingTable = builder.globalTable("hydration-tracking-topic", Consumed.with(Serdes.String(), offsetInfoSerde));

// The HydrationProcessor now additionally produces to the hydration-tracking-topic
public class HydrationProcessor implements Processor<String, Value> {
    // ... other methods ...

    @Override
    public void process(String key, Value value) {
        long currentOffset = context.offset();
        // Update the hydration-tracking-topic with the latest offset info
        context.forward(partitionId, new OffsetInfo(currentOffset, Math.max(currentOffset, highestSeenOffset)), To.child("hydration-tracking-topic"));
    }
}

// Check hydration status using the GlobalKTable
public boolean isHydrationComplete(StreamsBuilder builder) {
    ReadOnlyKeyValueStore<String, OffsetInfo> offsetStore = streams.store(hydrationTrackingTable.queryableStoreName(), QueryableStoreTypes.keyValueStore());
    for (String partition : partitions) {
        OffsetInfo offsetInfo = offsetStore.get(partition);
        if (offsetInfo.highestKnownOffset - offsetInfo.lastProcessedOffset > acceptableLag) {
            return false;
        }
    }
    return true;
}
