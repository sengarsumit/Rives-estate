package com.example.estate.Rives.estate.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KeepAliveSchedulerTest {

    @Test
    void ping_success_requestsTheConfiguredUrl() {
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(restClient.get().uri("https://example.onrender.com/actuator/health")
                .retrieve().toBodilessEntity())
                .thenReturn(ResponseEntity.ok().build());
        KeepAliveScheduler scheduler = new KeepAliveScheduler(restClient);
        ReflectionTestUtils.setField(scheduler, "url", "https://example.onrender.com/actuator/health");

        scheduler.ping();

        // atLeastOnce, not the default exactly-once: with RETURNS_DEEP_STUBS,
        // the when(...) setup above itself invokes .uri(...) once to record
        // the stub, so this call is already counted before ping() runs.
        verify(restClient.get(), atLeastOnce()).uri("https://example.onrender.com/actuator/health");
    }

    @Test
    void ping_requestFails_isCaughtAndDoesNotPropagate() {
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(restClient.get().uri(anyString()).retrieve().toBodilessEntity())
                .thenThrow(new RestClientException("connection refused"));
        KeepAliveScheduler scheduler = new KeepAliveScheduler(restClient);
        ReflectionTestUtils.setField(scheduler, "url", "https://example.onrender.com/actuator/health");

        scheduler.ping();
    }

    @Test
    void ping_blankUrl_skipsWithoutCallingTheClient() {
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        KeepAliveScheduler scheduler = new KeepAliveScheduler(restClient);
        ReflectionTestUtils.setField(scheduler, "url", "");

        scheduler.ping();

        verifyNoInteractions(restClient);
    }
}
