package no.novari.value.converting.infrastructure.messaging

import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.requestreply.ReplyProducerRecord
import no.novari.kafka.requestreply.RequestListenerConfiguration
import no.novari.kafka.requestreply.RequestListenerContainerFactory
import no.novari.kafka.requestreply.topic.RequestTopicService
import no.novari.kafka.requestreply.topic.configuration.RequestTopicConfiguration
import no.novari.kafka.requestreply.topic.name.RequestTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import no.novari.value.converting.domain.ValueConversion
import no.novari.value.converting.infrastructure.persistence.ValueConversionRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.time.Duration
import java.util.function.Function

@Configuration
class ValueConversionRequestConsumerConfiguration {
    @Bean
    fun valueConversionByIdRequestConsumer(
        requestTopicService: RequestTopicService,
        requestListenerContainerFactory: RequestListenerContainerFactory,
        valueConversionRepository: ValueConversionRepository,
        errorHandlerFactory: ErrorHandlerFactory,
    ): ConcurrentMessageListenerContainer<String, Long> {
        val requestTopicNameParameters =
            RequestTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).resourceName("value-converting")
                .parameterName("value-converting-id")
                .build()

        requestTopicService.createOrModifyTopic(
            requestTopicNameParameters,
            RequestTopicConfiguration
                .builder()
                .retentionTime(RETENTION_TIME)
                .build(),
        )

        val replyProducer =
            Function<ConsumerRecord<String, Long>, ReplyProducerRecord<ValueConversion>> { consumerRecord ->
                ReplyProducerRecord(
                    valueConversionRepository.findById(consumerRecord.value()).orElse(null),
                )
            }

        val requestListenerConfiguration =
            RequestListenerConfiguration
                .stepBuilder(Long::class.java)
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .build()

        return requestListenerContainerFactory
            .createRecordConsumerFactory(
                Long::class.java,
                ValueConversion::class.java,
                replyProducer,
                requestListenerConfiguration,
                errorHandlerFactory.createErrorHandler(
                    ErrorHandlerConfiguration
                        .stepBuilder<Long>()
                        .noRetries()
                        .skipFailedRecords()
                        .build(),
                ),
            ).createContainer(requestTopicNameParameters)
    }

    companion object {
        private val RETENTION_TIME: Duration = Duration.ofMinutes(10)
    }
}
