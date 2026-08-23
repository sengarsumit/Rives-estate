package com.example.estate.Rives.estate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Off by default (only active on Render, via APP_KEEP_ALIVE_ENABLED) - pings
// this app's own public /actuator/health URL, which Boot's default
// DataSourceHealthIndicator backs with a real DB connection check. That one
// request does double duty: it's genuine external HTTP traffic, which is
// what actually resets Render's free-tier 15-minute idle-shutdown timer (an
// in-process timer alone can't do that - Render's idle detection is based on
// inbound traffic, not on whether the process is busy), and the DB check
// keeps Neon's compute from auto-suspending after 5 minutes idle. Both
// thresholds are free-tier cost/resource-saving mechanisms, so this trades
// some of that free-tier budget for no cold starts - see DEPLOYMENT.md.
@Component
@ConditionalOnProperty(name = "app.keep-alive.enabled", havingValue = "true")
public class KeepAliveScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private final RestClient restClient;

    @Value("${app.keep-alive.url:}")
    private String url;

    public KeepAliveScheduler() {
        this(RestClient.create());
    }

    KeepAliveScheduler(RestClient restClient) {
        this.restClient = restClient;
    }

    @Scheduled(fixedDelayString = "${app.keep-alive.interval-ms:240000}")
    public void ping() {
        if (url.isBlank()) {
            logger.warn("app.keep-alive.enabled is true but app.keep-alive.url is not set - skipping ping");
            return;
        }
        try {
            restClient.get().uri(url).retrieve().toBodilessEntity();
        } catch (Exception e) {
            logger.warn("Keep-alive ping to {} failed: {}", url, e.getMessage());
        }
    }
}
