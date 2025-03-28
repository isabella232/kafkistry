package com.infobip.kafkistry.service.topic.wizard

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import com.infobip.kafkistry.service.assertAll
import com.infobip.kafkistry.service.newClusterInfo
import com.infobip.kafkistry.kafkastate.KafkaClusterState
import com.infobip.kafkistry.kafkastate.StateData
import com.infobip.kafkistry.kafkastate.StateType
import com.infobip.kafkistry.model.*
import com.infobip.kafkistry.service.generator.OverridesMinimizer
import com.infobip.kafkistry.service.propertiesForCluster
import com.infobip.kafkistry.service.resources.RequiredResourcesInspector
import com.infobip.kafkistry.service.topic.validation.NamingValidator
import com.infobip.kafkistry.model.ClusterRef
import com.infobip.kafkistry.model.KafkaClusterIdentifier
import com.infobip.kafkistry.model.Tag
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class TopicWizardConfigGeneratorTest {

    private val generator = TopicWizardConfigGenerator(
            Optional.empty(),
            OverridesMinimizer(),
            NamingValidator(),
            RequiredResourcesInspector(),
            TopicWizardProperties().apply {
                partitionThresholds = """
                    {
                      "default": {
                         "0": 12,
                         "500": 24,
                         "2000": 48,
                         "5000": 96
                      },
                      "overrides": {
                         "io": {
                            "0": 1,
                            "3000": 3
                         }
                      },
                      "tagOverrides": {
                         "cloud": {
                            "1000": 11,
                            "3000": 33
                         }
                      }
                    }
                    """.trimIndent()
            }
    )

    private val allClusters = mapOf(
        ClusterRef("mini") to clusterVisible("mini", listOf(1, 2)),
        ClusterRef("standard") to clusterVisible("standard", listOf(1, 2, 3)),
        ClusterRef("big") to clusterVisible("big", listOf(1, 2, 3, 4, 5, 6)),
        ClusterRef("io") to clusterVisible("io", listOf(1, 2, 3)),
        ClusterRef("aws", listOf("cloud")) to clusterVisible("aws", listOf(1, 2, 3)),
        ClusterRef("disabled") to StateData(StateType.DISABLED, "disabled", "cluster_state", System.currentTimeMillis()),
    )

    private fun clusterVisible(identifier: KafkaClusterIdentifier, nodes: List<Int>) = StateData(
            stateType = StateType.VISIBLE,
            clusterIdentifier = identifier,
            stateTypeName = "cluster_state",
            lastRefreshTime = System.currentTimeMillis(),
            value = KafkaClusterState(
                    clusterInfo = newClusterInfo(
                            identifier = identifier,
                            nodeIds = nodes,
                            onlineNodeIds = nodes,
                    ),
                    topics = emptyList(),
                    acls = emptyList()
            )
    )

    @Test
    fun `naming - default name`() {
        val topicDescription = generateTopicDescription()
        assertThat(topicDescription.name).isEqualTo("test-name")
    }

    @Test
    fun `partition count - message count per day 10k - (1 each 8s)`() {
        assertAll {
            assertThat(partitionCountForMessages(10_000, "mini")) shouldEqual 12
            assertThat(partitionCountForMessages(10_000, "standard")) shouldEqual 12
            assertThat(partitionCountForMessages(10_000, "big")) shouldEqual 12
            assertThat(partitionCountForMessages(10_000, "io")) shouldEqual 1
            assertThat(partitionCountForMessages(10_000, "aws", listOf("cloud"))) shouldEqual 1
        }
    }

    @Test
    fun `partition count - message count per day 1M - (11 per sec)`() {
        assertAll {
            assertThat(partitionCountForMessages(1_000_000, "mini")) shouldEqual 12
            assertThat(partitionCountForMessages(1_000_000, "standard")) shouldEqual 12
            assertThat(partitionCountForMessages(1_000_000, "big")) shouldEqual 12
            assertThat(partitionCountForMessages(1_000_000, "io")) shouldEqual 1
            assertThat(partitionCountForMessages(1_000_000, "aws", listOf("cloud"))) shouldEqual 1
        }
    }

    @Test
    fun `partition count - message count per day 20M - (231 per sec)`() {
        assertAll {
            assertThat(partitionCountForMessages(20_000_000, "mini")) shouldEqual 12
            assertThat(partitionCountForMessages(20_000_000, "standard")) shouldEqual 12
            assertThat(partitionCountForMessages(20_000_000, "big")) shouldEqual 12
            assertThat(partitionCountForMessages(20_000_000, "io")) shouldEqual 1
            assertThat(partitionCountForMessages(20_000_000, "aws", listOf("cloud"))) shouldEqual 1
        }
    }

    @Test
    fun `partition count - message count per day 100M - (1157 per sec)`() {
        assertAll {
            assertThat(partitionCountForMessages(100_000_000, "mini")) shouldEqual 24
            assertThat(partitionCountForMessages(100_000_000, "standard")) shouldEqual 24
            assertThat(partitionCountForMessages(100_000_000, "big")) shouldEqual 24
            assertThat(partitionCountForMessages(100_000_000, "io")) shouldEqual 1
            assertThat(partitionCountForMessages(100_000_000, "aws", listOf("cloud"))) shouldEqual 11
        }
    }

    @Test
    fun `partition count - message count per day 300M - (3472 per sec)`() {
        assertAll {
            assertThat(partitionCountForMessages(300_000_000, "mini")) shouldEqual 48
            assertThat(partitionCountForMessages(300_000_000, "standard")) shouldEqual 48
            assertThat(partitionCountForMessages(300_000_000, "big")) shouldEqual 48
            assertThat(partitionCountForMessages(300_000_000, "io")) shouldEqual 3
            assertThat(partitionCountForMessages(300_000_000, "aws", listOf("cloud"))) shouldEqual 33
        }
    }

    @Test
    fun `partition count - message count per day 1B - (11,6k per sec)`() {
        assertAll {
            assertThat(partitionCountForMessages(1_000_000_000, "mini")) shouldEqual 96
            assertThat(partitionCountForMessages(1_000_000_000, "standard")) shouldEqual 96
            assertThat(partitionCountForMessages(1_000_000_000, "big")) shouldEqual 96
            assertThat(partitionCountForMessages(1_000_000_000, "io")) shouldEqual 3
            assertThat(partitionCountForMessages(1_000_000_000, "aws", listOf("cloud"))) shouldEqual 33
        }
    }

    @Test
    fun `segment size continuity`() {
        assertAll {
            assertThat(generator.determineSegmentBytes(10_000_000L)) shouldEqual 5_000_000L        //small
            assertThat(generator.determineSegmentBytes(80_000_000L)) shouldEqual 40_000_000L       //small
            assertThat(generator.determineSegmentBytes(100_000_000L)) shouldEqual 50_000_000L      //small
            assertThat(generator.determineSegmentBytes(150_000_000L)) shouldEqual 52_428_800L      //small/big
            assertThat(generator.determineSegmentBytes(200_000_000L)) shouldEqual 52_428_800L      //small/big
            assertThat(generator.determineSegmentBytes(300_000_000L)) shouldEqual 52_428_800L      //small/big
            assertThat(generator.determineSegmentBytes(600_000_000L)) shouldEqual 60_000_000L      //big
            assertThat(generator.determineSegmentBytes(1_000_000_000L)) shouldEqual 100_000_000L   //big
            assertThat(generator.determineSegmentBytes(5_000_000_000L)) shouldEqual 500_000_000L   //big
        }
    }

    private fun partitionCountForMessages(
        messagesPerDay: Long, clusterIdentifier: KafkaClusterIdentifier, clusterTags: List<Tag> = emptyList()
    ): Int {
        val description = generateTopicDescription(
                messagesPerDay = messagesPerDay,
                kafkaClusterIdentifiers = listOf(clusterIdentifier)
        )
        val clusterRef = ClusterRef(clusterIdentifier, clusterTags)
        return description.propertiesForCluster(clusterRef).partitionCount
    }

    private fun generateTopicDescription(
            purpose: String = "The purpose of this topic",
            avgMessageSizeBytes: Int = 2000,
            messagesPerDay: Long = 10_000_000,
            producerServiceName: String = "test-service",
            retentionDays: Int = 15,
            teamName: String = "Team_Test",
            highAvailability: HighAvailability = HighAvailability.BASIC,
            kafkaClusterIdentifiers: List<KafkaClusterIdentifier> = listOf("mini")
    ): TopicDescription {
        return generator.generateTopicDescription(
                wizardAnswers = TopicCreationWizardAnswers(
                        purpose, teamName, producerServiceName,
                        TopicNameMetadata(
                                attributes = mapOf("name" to "test-name")
                        ),
                        ResourceRequirements(
                                MsgSize(avgMessageSizeBytes, BytesUnit.B),
                                DataRetention(retentionDays, TimeUnit.DAYS),
                                emptyMap(), emptyMap(),
                                MessagesRate(messagesPerDay, ScaleFactor.ONE, RateUnit.MSG_PER_DAY),
                                emptyMap(), emptyMap()
                        ),
                        highAvailability,
                        presence = Presence(PresenceType.INCLUDED_CLUSTERS, kafkaClusterIdentifiers)
                ),
                allClusters = allClusters,
        )
    }

    private infix fun <A : AbstractObjectAssert<*, *>> A.shouldEqual(expected: Any?) = this.isEqualTo(expected)
}