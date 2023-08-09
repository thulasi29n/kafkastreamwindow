package com.example.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    // Common configurations
    @Value("${kafka.bootstrapServers}")
    private String bootstrapServers;
    @Value("${kafka.sasl.jaas.config}")
    private String jaasConfig;

    // Kafka Streams specific
    @Value("${kafka.streams.applicationId}")
    private String applicationId;

    // Consumer specific
    @Value("${kafka.consumer.groupId}")
    private String consumerGroupId;

    // Note: Add more @Value fields as needed to pull other configurations

    @Bean(name = "kStreamsConfigs")
    public Map<String, Object> kStreamsConfigs() {
        Map<String, Object> props = commonConfigs();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        // Add more Kafka Streams specific configurations if needed
        return props;
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = commonConfigs();
        // Add any other producer-specific configurations
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = commonConfigs();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        // Add any other consumer-specific configurations
        return props;
    }

    @Bean(name = "streamsBuilder")
    public StreamsBuilderFactoryBean streamsBuilder() {
        StreamsBuilderFactoryBean streamsBuilderFactoryBean = new StreamsBuilderFactoryBean();
        
        // Create a combined configuration map that includes Kafka Streams, producer, and consumer configurations.
        Map<String, Object> combinedConfig = new HashMap<>();
        combinedConfig.putAll(kStreamsConfigs());
        combinedConfig.putAll(producerConfigs());
        combinedConfig.putAll(consumerConfigs());

        streamsBuilderFactoryBean.setStreamsConfiguration(combinedConfig);
        return streamsBuilderFactoryBean;
    }

    private Map<String, Object> commonConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config", jaasConfig);
        props.put("ssl.endpoint.identification.algorithm", "https");
        // Add more common configurations if needed
        return props;
    }
}
