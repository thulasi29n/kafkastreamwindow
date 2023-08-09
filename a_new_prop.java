spring:
  kafka:
    # General Kafka configurations (common to producer, consumer, and Kafka Streams)
    bootstrap-servers: your-bootstrap-server
    
    # Producer-specific configurations
    producer:
      compression.type: snappy
      linger.ms: 5
      batch.size: 16384
    
    # Consumer-specific configurations
    consumer:
      max.poll.records: 1000
      auto.offset.reset: earliest
      fetch.min.bytes: 50000
      
    # Kafka Streams specific configurations
    streams:
      application.id: my-kafka-streams-app
      properties:
        # If needed, you can specify more detailed properties here
        security.protocol: SASL_SSL
        sasl.mechanism: PLAIN
        sasl.jaas.config: org.apache.kafka.common.security.plain.PlainLoginModule required username="YOUR_USERNAME" password="YOUR_PASSWORD";
