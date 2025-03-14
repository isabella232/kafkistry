package com.infobip.kafkistry.kafkastate

import io.prometheus.client.Gauge
import io.prometheus.client.Summary
import com.infobip.kafkistry.kafkastate.config.PoolingProperties
import com.infobip.kafkistry.model.KafkaCluster
import com.infobip.kafkistry.model.KafkaClusterIdentifier
import com.infobip.kafkistry.repository.KafkaClustersRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.CustomizableThreadFactory
import java.util.concurrent.*

private val clusterPoolingSummary = Summary.build()
        .name("kafkistry_cluster_pooling")
        .help("Summary of latencies of each refresh attempt for cluster")
        .labelNames("cluster_identifier", "pooling_type", "cluster_status")
        .ageBuckets(5)
        .maxAgeSeconds(TimeUnit.MINUTES.toSeconds(5))
        .quantile(0.5, 0.05)   // Add 50th percentile (= median) with 5% tolerated error
        .quantile(0.9, 0.01)   // Add 90th percentile with 1% tolerated error
        .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
        .register()

private val clusterPoolingTypeOkGauge = Gauge.build()
        .name("kafkistry_cluster_pooling_status")
        .help("Gauge telling if cluster pooling status is ok (1.0) or not ok (0.0)")
        .labelNames("cluster_identifier", "pooling_type")
        .register()

abstract class BaseKafkaStateProvider(
        private val clustersRepository: KafkaClustersRepository,
        private val clusterFilter: ClusterEnabledFilter,
        poolingProperties: PoolingProperties
) : AutoCloseable {

    protected val log = LoggerFactory.getLogger(this.javaClass)!!

    private val executor = Executors.newFixedThreadPool(
            poolingProperties.clusterConcurrency,
            CustomizableThreadFactory("cluster-scraper-").apply { this.isDaemon = false }
    )

    override fun close() {
        log.info("Closing {}", this)
        executor.shutdown()
    }

    @Scheduled(fixedRateString = "#{poolingProperties.intervalMs()}")
    open fun refreshClustersStates() = doRefreshClustersStates()

    protected fun doRefreshClustersStates() {
        val clusters = clustersRepository.findAll()
        val (enabledClusters, disabledClusters) = clusters.partition { clusterFilter.enabled(it.identifier) }
        setupCachedState(
                enabledClusters.map { it.identifier },
                disabledClusters.map { it.identifier }
        )
        enabledClusters
                .map { CompletableFuture.supplyAsync({ refreshCluster(it) }, executor) }
                .let { CompletableFuture.allOf(*it.toTypedArray()) }
                .get()
    }

    fun refreshClusterState(clusterIdentifier: KafkaClusterIdentifier) {
        val cluster = clustersRepository.findById(clusterIdentifier)
        if (cluster != null) {
            refreshCluster(cluster)
        }
    }

    private fun refreshCluster(cluster: KafkaCluster) {
        val refresh = doRefreshCluster(cluster)
        clusterPoolingSummary.labels(cluster.identifier, refresh.scrapeType, refresh.clusterState.name).observe(refresh.durationMs.toDouble())
        val ok = when (refresh.clusterState) {
            StateType.VISIBLE, StateType.DISABLED -> 1.0
            StateType.UNREACHABLE, StateType.UNKNOWN, StateType.INVALID_ID -> 0.0
        }
        clusterPoolingTypeOkGauge.labels(cluster.identifier, refresh.scrapeType).set(ok)
    }

    protected abstract fun setupCachedState(
        enabledClusters: List<KafkaClusterIdentifier>,
        disabledClusters: List<KafkaClusterIdentifier>
    )

    protected abstract fun doRefreshCluster(kafkaCluster: KafkaCluster): RefreshStatus

}

data class RefreshStatus(
        val scrapeType: String,
        val clusterState: StateType,
        val durationMs: Long
)
