package ru.practicum.collector.config;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import jakarta.annotation.PostConstruct;
import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private final MockSchemaRegistryClient schemaRegistryClient = new MockSchemaRegistryClient();

    @PostConstruct
    public void registerSchemas() throws Exception {
        Schema userActionSchema = UserActionAvro.getClassSchema();
        schemaRegistryClient.register("stats.user-actions.v1-value", userActionSchema);
    }

    @Bean
    public SchemaRegistryClient schemaRegistryClient() {
        return schemaRegistryClient;
    }

    @Bean
    public ProducerFactory<String, UserActionAvro> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put("schema.registry.url", "mock://test");

        DefaultKafkaProducerFactory<String, UserActionAvro> factory =
                new DefaultKafkaProducerFactory<>(props);
        factory.setValueSerializerSupplier(() -> {
            KafkaAvroSerializer serializer = new KafkaAvroSerializer(schemaRegistryClient);
            serializer.configure(props, false);
            return (Serializer) serializer;
        });
        return factory;
    }

    @Bean
    public KafkaTemplate<String, UserActionAvro> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}