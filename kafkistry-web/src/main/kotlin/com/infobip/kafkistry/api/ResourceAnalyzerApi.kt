package com.infobip.kafkistry.api

import com.infobip.kafkistry.model.KafkaClusterIdentifier
import com.infobip.kafkistry.model.TopicDescription
import com.infobip.kafkistry.model.TopicName
import com.infobip.kafkistry.service.OptionalValue
import com.infobip.kafkistry.service.resources.ClusterDiskUsage
import com.infobip.kafkistry.service.resources.ClusterResourcesAnalyzer
import com.infobip.kafkistry.service.resources.TopicDiskUsage
import com.infobip.kafkistry.service.resources.TopicResourcesAnalyzer
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("\${app.http.root-path}/api/resource-analyzer")
class ResourceAnalyzerApi(
    private val clusterResourcesAnalyzer: ClusterResourcesAnalyzer,
    private val topicResourcesAnalyzer: TopicResourcesAnalyzer,
) {

    @GetMapping("/cluster/resources")
    fun getClusterStatus(
        @RequestParam("clusterIdentifier") clusterIdentifier: KafkaClusterIdentifier
    ): ClusterDiskUsage = clusterResourcesAnalyzer.clusterDiskUsage(clusterIdentifier)

    @GetMapping("/topic/cluster/resources")
    fun getTopicStatusOnCluster(
        @RequestParam("clusterIdentifier") clusterIdentifier: KafkaClusterIdentifier,
        @RequestParam("topicName") topicName: TopicName,
    ): TopicDiskUsage = topicResourcesAnalyzer.topicOnClusterDiskUsage(topicName, clusterIdentifier)

    @PostMapping("/topic/resources")
    fun getTopicStatus(
        @RequestBody topicDescription: TopicDescription,
    ): Map<KafkaClusterIdentifier, OptionalValue<TopicDiskUsage>> = topicResourcesAnalyzer.topicDryRunDiskUsage(topicDescription)
}