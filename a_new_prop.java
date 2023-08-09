spring:
  kafka:
    bootstrap-servers: your-bootstrap-server
    
    # Kafka Streams specific configurations
    streams:
      properties:
        application.id: my-kafka-streams-app

        # Producer-specific properties
        compression.type: snappy
        linger.ms: 5
        batch.size: 16384

        # Consumer-specific properties
        max.poll.records: 1000
        auto.offset.reset: earliest
        fetch.min.bytes: 50000

        # Security properties
        security.protocol: SASL_SSL
        sasl.mechanism: PLAIN
        sasl.jaas.config: org.apache.kafka.common.security.plain.PlainLoginModule required username="YOUR_USERNAME" password="YOUR_PASSWORD";
