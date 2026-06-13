package ru.practicum.analyzer.config;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import jakarta.annotation.PostConstruct;
import org.apache.avro.Schema;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
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

        Schema eventSimilaritySchema = EventSimilarityAvro.getClassSchema();
        schemaRegistryClient.register("stats.events-similarity.v1-value", eventSimilaritySchema);
    }

    @Bean
    public SchemaRegistryClient schemaRegistryClient() {
        return schemaRegistryClient;
    }

    @Bean
    public ConsumerFactory<String, UserActionAvro> userActionConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-user-actions");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("specific.avro.reader", true);

        DefaultKafkaConsumerFactory<String, UserActionAvro> factory =
                new DefaultKafkaConsumerFactory<>(props);
        factory.setValueDeserializerSupplier(() -> {
            Deserializer<UserActionAvro> deserializer = (Deserializer) new KafkaAvroDeserializer(schemaRegistryClient);
            deserializer.configure(props, false);
            return deserializer;
        });
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> userActionListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userActionConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, EventSimilarityAvro> eventSimilarityConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-similarity");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("specific.avro.reader", true);

        DefaultKafkaConsumerFactory<String, EventSimilarityAvro> factory =
                new DefaultKafkaConsumerFactory<>(props);
        factory.setValueDeserializerSupplier(() -> {
            Deserializer<EventSimilarityAvro> deserializer = (Deserializer) new KafkaAvroDeserializer(schemaRegistryClient);
            deserializer.configure(props, false);
            return deserializer;
        });
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> eventSimilarityListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventSimilarityConsumerFactory());
        return factory;
    }
}