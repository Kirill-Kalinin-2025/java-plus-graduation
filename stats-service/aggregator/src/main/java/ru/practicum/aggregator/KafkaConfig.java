package ru.practicum.aggregator;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import jakarta.annotation.PostConstruct;
import org.apache.avro.Schema;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
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
    public ConsumerFactory<String, UserActionAvro> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
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
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ProducerFactory<String, EventSimilarityAvro> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        DefaultKafkaProducerFactory<String, EventSimilarityAvro> factory =
                new DefaultKafkaProducerFactory<>(props);
        factory.setValueSerializerSupplier(() -> {
            Serializer<EventSimilarityAvro> serializer = (Serializer) new KafkaAvroSerializer(schemaRegistryClient);
            serializer.configure(props, false);
            return serializer;
        });
        return factory;
    }

    @Bean
    public KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}