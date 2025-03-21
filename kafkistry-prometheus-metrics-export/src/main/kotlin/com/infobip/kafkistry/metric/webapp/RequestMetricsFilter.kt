package com.infobip.kafkistry.metric.webapp

import io.prometheus.client.Summary
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.servlet.HandlerMapping
import java.util.concurrent.TimeUnit
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

private val httpRequestLatencies = Summary.build()
        .name("kafkistry_http_latencies")
        .help("Summary of latencies of each http request broken down per separate html method and uri path")
        .labelNames("http_method", "http_uri")
        .ageBuckets(5)
        .maxAgeSeconds(TimeUnit.MINUTES.toSeconds(5))
        .quantile(0.5, 0.05)   // Add 50th percentile (= median) with 5% tolerated error
        .quantile(0.9, 0.01)   // Add 90th percentile with 1% tolerated error
        .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
        .register()

@Component
@ConditionalOnProperty("app.metrics.http-calls", matchIfMissing = true)
class RequestMetricsFilter : GenericFilterBean() {

    private val log = LoggerFactory.getLogger(RequestMetricsFilter::class.java)

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val start = System.currentTimeMillis()
        try {
            chain.doFilter(request, response)
        } finally {
            val end = System.currentTimeMillis()
            recordExecutionTime(request, end - start)
        }
    }

    private fun recordExecutionTime(request: ServletRequest, durationMs: Long) {
        if (request !is HttpServletRequest) {
            return
        }
        val requestPath = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
            ?.let { it as? String }
            ?.takeIf { it != "'null'" }
            ?: request.requestURI
        val requestLabel = request.method + " " + requestPath
        log.info("Execution duration '{}' is {} ms", requestLabel, durationMs)
        httpRequestLatencies.labels(request.method, requestPath).observe(durationMs.toDouble())
    }

}