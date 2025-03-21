package com.infobip.kafkistry.kafkastate

import com.infobip.kafkistry.kafka.KafkaClientProvider
import com.infobip.kafkistry.kafkastate.config.PoolingProperties
import com.infobip.kafkistry.model.KafkaCluster
import com.infobip.kafkistry.repository.KafkaClustersRepository
import com.infobip.kafkistry.service.background.BackgroundJobIssuesRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import kotlin.math.max

@Component
class KafkaTopicOffsetsProvider(
    clustersRepository: KafkaClustersRepository,
    clusterFilter: ClusterEnabledFilter,
    issuesRegistry: BackgroundJobIssuesRegistry,
    private val poolingProperties: PoolingProperties,
    private val clientProvider: KafkaClientProvider
) : AbstractKafkaStateProvider<ClusterTopicOffsets>(
    clustersRepository, clusterFilter, poolingProperties, issuesRegistry
) {

    override val stateTypeName = "topic_offsets"

    override fun fetchState(kafkaCluster: KafkaCluster): ClusterTopicOffsets {
        val topicNames = clientProvider.doWithClient(kafkaCluster) { it.listAllTopicNames().get() }
        val topicsOffsets = topicNames
            .chunked(max(20, topicNames.size / clientProvider.perClusterConcurrency()))
            .map { topicsBatch -> clientProvider.doWithClient(kafkaCluster) { it.topicsOffsets(topicsBatch) } }
            .apply { CompletableFuture.allOf(*toTypedArray()).get() }
            .flatMap { batchFuture -> batchFuture.get().map { it.toPair() } }
            .associate { it }
        return ClusterTopicOffsets(
            topicsOffsets = topicsOffsets
        )
    }

}
