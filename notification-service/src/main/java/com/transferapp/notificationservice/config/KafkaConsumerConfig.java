package com.transferapp.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    public static final String TRANSFER_COMPLETED_DLT =
            "transfer-completed-dlt";

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<Object, Object> kafkaTemplate
    ) {
        return new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                        new TopicPartition(
                                TRANSFER_COMPLETED_DLT,
                                record.partition()
                        )
        );
    }

    @Bean
    public NewTopic transferCompletedDltTopic() {
        return TopicBuilder
                .name(TRANSFER_COMPLETED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer
    ) {

        FixedBackOff fixedBackOff =
                new FixedBackOff(
                        1000L,
                        2L
                );

        return new DefaultErrorHandler(
                recoverer,
                fixedBackOff
        );
    }
}