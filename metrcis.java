import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KStream;
import org.apache.kafka.streams.KTable;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.common.metrics.Metric;
import org.apache.kafka.common.metrics.MetricName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class KafkaStreamsComponent {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamsComponent.class);

    @Autowired
    private StreamsBuilderFactoryBean factoryBean;

    public void buildTopology() {
        StreamsBuilder builder = factoryBean.getObject();

        KStream<String, Regulator> source = builder.stream("input-topic", Consumed.with(Serdes.String(), new JsonSerde<>(Regulator.class)));

        KTable<String, RegulatorAggregate> aggregateTable = source.groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(Regulator.class)))
                .aggregate(
                        RegulatorAggregate::new,
                        (key, newValue, aggValue) -> {
                            aggValue.incrementCount();
                            aggValue.setLatestValue(newValue.getMaxInstructionCount());
                            return aggValue;
                        },
                        Materialized.<String, RegulatorAggregate, KeyValueStore<Bytes, byte[]>>as("aggregation-store")
                                .withValueSerde(new JsonSerde<>(RegulatorAggregate.class))
                );

        aggregateTable.toStream().to("output-topic", Produced.with(Serdes.String(), new JsonSerde<>(RegulatorAggregate.class)));
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        logMetrics();
    }

    @Scheduled(fixedDelay = 10000)  // Log metrics every 10 seconds
    public void logMetricsPeriodically() {
        logMetrics();
    }

    private void logMetrics() {
        KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();
        if (kafkaStreams != null) {
            Map<MetricName, ? extends Metric> metrics = kafkaStreams.metrics();
            for (Map.Entry<MetricName, ? extends Metric> entry : metrics.entrySet()) {
                MetricName metricName = entry.getKey();
                Metric metric = entry.getValue();
                logger.info("Metric Name = {}, Value = {}", metricName.name(), metric.metricValue());
            }
        }
    }
}
