package ru.practicum.analyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, byte[]> userActionConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-user-actions-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> userActionListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userActionConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, byte[]> eventSimilarityConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-similarity-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> eventSimilarityListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventSimilarityConsumerFactory());
        return factory;
    }

    @Bean
    public ApplicationRunner seekToEndUserActions(ConsumerFactory<String, byte[]> userActionConsumerFactory) {
        return args -> {
            try (var consumer = userActionConsumerFactory.createConsumer("analyzer-reset-ua", "reset")) {
                var partitions = consumer.partitionsFor("stats.user-actions.v1")
                        .stream()
                        .map(p -> new TopicPartition(p.topic(), p.partition()))
                        .collect(Collectors.toList());
                if (!partitions.isEmpty()) {
                    consumer.assign(partitions);
                    consumer.seekToEnd(partitions);
                    consumer.commitSync();
                    log.info("Analyzer user-actions offset reset to end");
                }
            }
        };
    }

    @Bean
    public ApplicationRunner seekToEndEventSimilarity(ConsumerFactory<String, byte[]> eventSimilarityConsumerFactory) {
        return args -> {
            try (var consumer = eventSimilarityConsumerFactory.createConsumer("analyzer-reset-es", "reset")) {
                var partitions = consumer.partitionsFor("stats.events-similarity.v1")
                        .stream()
                        .map(p -> new TopicPartition(p.topic(), p.partition()))
                        .collect(Collectors.toList());
                if (!partitions.isEmpty()) {
                    consumer.assign(partitions);
                    consumer.seekToEnd(partitions);
                    consumer.commitSync();
                    log.info("Analyzer event-similarity offset reset to end");
                }
            }
        };
    }
}