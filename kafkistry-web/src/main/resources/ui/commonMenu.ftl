
<#-- @ftlvariable name="appUrl" type="com.infobip.kafkistry.webapp.url.AppUrl" -->
<#-- @ftlvariable name="user"  type="com.infobip.kafkistry.webapp.security.User" -->
<#-- @ftlvariable name="_csrf" type="org.springframework.security.web.csrf.CsrfToken" -->
<#-- @ftlvariable name="gitStorageEnabled"  type="java.lang.Boolean" -->
<#-- @ftlvariable name="securityEnabled"  type="java.lang.Boolean" -->
<#-- @ftlvariable name="menuItems"  type="java.util.List<com.infobip.kafkistry.webapp.menu.MenuItem>" -->
<#-- @ftlvariable name="backgroundJobIssues"  type="java.util.List<com.infobip.kafkistry.service.background.BackgroundJobIssue>" -->
<#-- @ftlvariable name="lastCommit"  type="java.lang.String" -->
<#-- @ftlvariable name="imageProps"  type="com.infobip.kafkistry.webapp.ImageProperties" -->

<#import "common/documentation.ftl" as _doc>
<#import "common/infoIcon.ftl" as _info>
<#import "common/util.ftl" as _util>

<nav class="navbar navbar-expand-sm navbar-light bg-light nav-tabs mb-0 pb-0">

  <div class="collapse navbar-collapse" id="kafkistry-navbar">
    <ul class="navbar-nav mr-auto">
      <li class="nav-item">
        <a id="nav-home" class="nav-link" href="${appUrl.main().url()}">
            &nbsp;
            <img src="static/img/${imageProps.dirPath}/${imageProps.logo}?ver=${lastCommit}"
                 style="margin: -5px; height: 1.9em;"
                 alt="Home" title="Home"/>
            &nbsp;
            <span class="sr-only">(current)</span>
        </a>
      </li>
      <li class="nav-item">
          <a id="nav-clusters" class="nav-link" href="${appUrl.clusters().showClusters()}">Clusters<span class="sr-only">(current)</span></a>
      </li>
      <li class="nav-item">
        <a id="nav-topics" class="nav-link" href="${appUrl.topics().showTopics()}">Topics<span class="sr-only">(current)</span></a>
      </li>
      <li class="nav-item">
        <a id="nav-acls" class="nav-link" href="${appUrl.acls().showAll()}">Acls<span class="sr-only">(current)</span></a>
      </li>
      <li class="nav-item">
        <a id="nav-quotas" class="nav-link" href="${appUrl.quotas().showAll()}">Quotas<span class="sr-only">(current)</span></a>
      </li>
      <li class="nav-item">
        <a id="nav-consumer-groups" class="nav-link" href="${appUrl.consumerGroups().showAllClustersConsumerGroups()}">Consumer groups<span class="sr-only">(current)</span></a>
      </li>
      <li class="nav-item">
        <a id="nav-consume" class="nav-link" href="${appUrl.consumeRecords().showConsumePage()}">Consume<span class="sr-only">(current)</span></a>
      </li>
      <li class="nav-item">
        <a id="nav-records-structure" class="nav-link" href="${appUrl.recordsStructure().showMenuPage()}">Records structure<span class="sr-only">(current)</span></a>
      </li>
      <li class="nav-item">
        <a id="nav-kstream" class="nav-link" href="${appUrl.kStream().showAll()}">KStream<span class="sr-only">(current)</span></a>
      </li>
      <li class="nav-item">
        <a id="nav-sql" class="nav-link" href="${appUrl.sql().showSqlPage()}">SQL<span class="sr-only">(current)</span></a>
      </li>
      <#if gitStorageEnabled>
          <li class="nav-item">
              <a id="nav-history" class="nav-link" href="${appUrl.history().showRecent()}">Git history<span class="sr-only">(current)</span></a>
          </li>
      </#if>
      <#list menuItems as menuItem>
        <li class="nav-item">
          <a id="${menuItem.id}" class="nav-link" href="${appUrl.menuItem(menuItem)}">${menuItem.name} <#if menuItem.newItem><@_util.newTag/></#if><span class="sr-only">(current)</span></a>
        </li>
      </#list>
      <li class="nav-item">
          <a id="nav-app-info" class="nav-link" href="${appUrl.about().showAboutPage()}">About<span class="sr-only">(current)</span></a>
      </li>

    </ul>

    <#if user??>
      <div>
        <strong>${user.fullName}</strong>
        <#assign userTooltip>
            <table class='table mb-0'>
              <tr>
                <td><strong>Username</strong>: ${user.username}</td>
              </tr>
              <tr>
                <td><strong>Email</strong>: ${user.email}</td>
              </tr>
              <tr>
                <td><strong>Role</strong>: <code>${user.role.authority}</code></td>
              </tr>
              <tr>
                <td><strong>Authorities</strong>:
                  <br/>
                  <#list user.authorities as authority>
                    <code>${authority.authority}</code> <#if !authority?is_last><br/></#if>
                  </#list>
                </td>
              </tr>
            </table>
        </#assign>
        <@_info.icon tooltip=userTooltip/>
      </div>
    </#if>

    <#if securityEnabled>
      <div class="ml-3">
        <form method="post" action="logout" style="display: inline-block; margin: 0">
          <#if _csrf??>
            <input name="${_csrf.parameterName}" type="hidden" value="${_csrf.token}"/>
          </#if>
          <input type="submit" value="Logout" class="btn btn-danger">
        </form>
      </div>
    </#if>

  </div>
</nav>

<script>
    function showMoreIssues() {
        $('#more-issues-btn').hide();
        $('.issue').show();
    }
</script>

<div class="container">
    <#assign maxExpanded = 0>
    <#assign maxShown = 7>
    <#assign collapsedClass = (backgroundJobIssues?size lte maxExpanded)?then("", "collapsed")>
    <#assign expandedClass = (backgroundJobIssues?size lte maxExpanded)?then("show", "")>
    <#list backgroundJobIssues as issue>
        <div class="alert alert-danger issue ${collapsedClass}" role="alert" data-target=".issue-msg-${issue?index}" data-toggle="collapsing"
        <#if issue?index gte maxShown>style="display: none;" </#if>>
            <span class="when-collapsed" title="expand...">▼</span>
            <span class="when-not-collapsed" title="collapse...">△</span>
            <span class="message">Background job failed: ${issue.jobName}</span>
            <pre class="pre-message issue-msg-${issue?index} collapseable ${expandedClass}">${issue.failureMessage}</pre>
        </div>
        <#if !issue?is_last && issue?index == maxShown - 1>
            <#assign leftIssues = backgroundJobIssues?size - maxShown>
            <div id="more-issues-btn">
                <button class="btn btn-sm btn-outline-danger" onclick="showMoreIssues();">
                    And ${leftIssues} more failed background jobs...
                </button>
                <br/>
                <br/>
            </div>
        </#if>
    </#list>
</div>

