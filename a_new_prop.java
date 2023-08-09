spring:
  kafka:
    streams:
      properties:
        # Common Kafka Streams configurations
        application.id: my-kafka-streams-app

        # Producer-specific configurations for Kafka Streams
        producer.compression.type: snappy
        producer.linger.ms: 5
        producer.batch.size: 16384
        
        # Consumer-specific configurations for Kafka Streams
        consumer.max.poll.records: 1000
        consumer.auto.offset.reset: earliest
        consumer.fetch.min.bytes: 50000

        # SASL and security if needed
        security.protocol: SASL_SSL
        sasl.mechanism: PLAIN
        sasl.jaas.config: org.apache.kafka.common.security.plain.PlainLoginModule required username="YOUR_USERNAME" password="YOUR_PASSWORD";
