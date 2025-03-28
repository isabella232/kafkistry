<#-- @ftlvariable name="appUrl" type="com.infobip.kafkistry.webapp.url.AppUrl" -->
<#-- @ftlvariable name="clusterInfo" type="com.infobip.kafkistry.kafka.ClusterInfo" -->
<#-- @ftlvariable name="topicName" type="java.lang.String" -->
<#-- @ftlvariable name="assignmentStatus" type="com.infobip.kafkistry.service.PartitionsAssignmentsStatus" -->
<#-- @ftlvariable name="reBalanceSuggestion" type="com.infobip.kafkistry.service.ReBalanceSuggestion" -->
<#-- @ftlvariable name="topicReplicas" type="com.infobip.kafkistry.service.replicadirs.TopicReplicaInfos" -->

<table class="table">
    <tr>
        <th>Cluster</th>
        <td>
            <a href="${appUrl.clusters().showCluster(clusterInfo.identifier)}">${clusterInfo.identifier}</a>
        </td>
    </tr>
    <tr>
        <th>Topic</th>
        <td>
            <a href="${appUrl.topics().showTopic(topicName)}">${topicName}</a>
        </td>
    </tr>
    <#assign dataMigration = reBalanceSuggestion.dataMigration>
    <#include "assignmentChangeStats.ftl">
    <#assign oldDisbalance = reBalanceSuggestion.oldDisbalance>
    <#assign newDisbalance = reBalanceSuggestion.newDisbalance>
    <tr>
        <th>Replicas disbalance</th>
        <td>
            Old: ${oldDisbalance.replicasDisbalance} (${oldDisbalance.replicasDisbalancePercent?string["0.##"]}% of
            all replicas)
            New: ${newDisbalance.replicasDisbalance} (${newDisbalance.replicasDisbalancePercent?string["0.##"]}% of
            all replicas)
        </td>
    </tr>
    <tr>
        <th>Leaders disbalance</th>
        <td>
            Old: ${oldDisbalance.leadersDisbalance} (${oldDisbalance.leadersDisbalancePercent?string["0.##"]}% of
            all partitions)
            New: ${newDisbalance.leadersDisbalance} (${newDisbalance.leadersDisbalancePercent?string["0.##"]}% of
            all partitions)
        </td>
    </tr>
</table>

<#include "../topics/partitionReplicaAssignments.ftl">

<#assign assignments = reBalanceSuggestion.assignmentsChange.newAssignments>
<#include "assignmentData.ftl">
