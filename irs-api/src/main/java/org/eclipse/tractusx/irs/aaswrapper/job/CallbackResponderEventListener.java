/********************************************************************************
 * Copyright (c) 2022,2024
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 *       2022,2024: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022,2023: BOSCH AG
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.irs.aaswrapper.job;

import static org.eclipse.tractusx.irs.configuration.RestTemplateConfig.NO_ERROR_REST_TEMPLATE;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.tractusx.irs.common.JobProcessingFinishedEvent;
import org.eclipse.tractusx.irs.component.enums.JobState;
import org.eclipse.tractusx.irs.component.enums.ProcessingState;
import org.eclipse.tractusx.irs.services.events.BatchOrderProcessingFinishedEvent;
import org.eclipse.tractusx.irs.services.events.BatchProcessingFinishedEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Listens for JobProcessingFinishedEvent, BatchProcessingFinishedEvent
 * and BatchOrderProcessingFinishedEvent.
 * Calling callbackUrl with notification to requestor.
 * Execution is done in a separate thread.
 */
@Slf4j
@Service
class CallbackResponderEventListener {

    public static final String INVALID_CALLBACK_URL = "Invalid callback url '{}'.";
    private final UrlValidator urlValidator;
    private final RestTemplate restTemplate;
    private final Map<String, Long> completedCallbacks;

    /* package */ CallbackResponderEventListener(
            @Qualifier(NO_ERROR_REST_TEMPLATE) final RestTemplate noErrorRestTemplate) {
        this.urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
        this.restTemplate = noErrorRestTemplate;
        this.completedCallbacks = new HashMap<>();
    }

    @Async
    @EventListener
    public void handleJobProcessingFinishedEvent(final JobProcessingFinishedEvent jobProcessingFinishedEvent) {
        if (StringUtils.isBlank(jobProcessingFinishedEvent.callbackUrl())) {
            return;
        }
        log.info("Processing of job has finished - attempting to notify job requestor");

        final URI callbackUri = buildCallbackUri(jobProcessingFinishedEvent.callbackUrl(),
                jobProcessingFinishedEvent.jobId(), JobState.valueOf(jobProcessingFinishedEvent.jobState()));

        if (!urlValidator.isValid(callbackUri.toString())) {
            log.warn(INVALID_CALLBACK_URL, callbackUri);
            return;
        }

        log.info("Got callback url '{}' for jobId '{}' with state '{}'", callbackUri, jobProcessingFinishedEvent.jobId(),
                jobProcessingFinishedEvent.jobState());
        sendCallback(callbackUri, jobProcessingFinishedEvent.jobId());
    }

    @Async
    @EventListener
    public void handleBatchProcessingFinishedEvent(final BatchProcessingFinishedEvent batchProcessingFinishedEvent) {
        if (StringUtils.isBlank(batchProcessingFinishedEvent.callbackUrl())) {
            return;
        }
        log.info("Processing of Batch has finished - attempting to notify requestor");

        final URI callbackUri = buildCallbackUri(batchProcessingFinishedEvent.callbackUrl(),
                batchProcessingFinishedEvent.batchOrderId(), batchProcessingFinishedEvent.batchId(),
                batchProcessingFinishedEvent.batchOrderState(), batchProcessingFinishedEvent.batchState());

        if (!urlValidator.isValid(callbackUri.toString())) {
            log.warn(INVALID_CALLBACK_URL, callbackUri);
            return;
        }

        log.info("Got callback url '{}' for orderId '{}' with orderState '{}' and batchId '{}' with batchState '{}'", callbackUri,
                batchProcessingFinishedEvent.batchOrderId(), batchProcessingFinishedEvent.batchOrderState(),
                batchProcessingFinishedEvent.batchId(), batchProcessingFinishedEvent.batchState());
        sendCallback(callbackUri, batchProcessingFinishedEvent.batchId().toString());
    }

    @Async
    @EventListener
    public void handleBatchOrderProcessingFinishedEvent(
            final BatchOrderProcessingFinishedEvent batchOrderProcessingFinishedEvent) {
        if (StringUtils.isBlank(batchOrderProcessingFinishedEvent.callbackUrl())) {
            return;
        }
        log.info("Processing of Batch Order has finished - attempting to notify requestor");

        final URI callbackUri = buildCallbackUri(batchOrderProcessingFinishedEvent.callbackUrl(),
                batchOrderProcessingFinishedEvent.batchOrderId(), null,
                batchOrderProcessingFinishedEvent.batchOrderState(), null);
        if (!urlValidator.isValid(callbackUri.toString())) {
            log.warn(INVALID_CALLBACK_URL, callbackUri);
            return;
        }

        log.info("Got callback url '{}' for orderId '{}' with orderState '{}'", callbackUri,
                batchOrderProcessingFinishedEvent.batchOrderId(), batchOrderProcessingFinishedEvent.batchOrderState());
        sendCallback(callbackUri, batchOrderProcessingFinishedEvent.batchOrderId().toString());

    }

    private void sendCallback(final URI callbackUri, final String key) {
        if (callbackNotSentYet(key)) {
            addJobToSentCallbacks(key);
            cleanupValuesOlderThan(Duration.ofHours(1));
            try {
                final ResponseEntity<Void> callbackResponse = restTemplate.getForEntity(callbackUri, Void.class);
                log.info("Callback url '{}' pinged, received http status: '{}'", callbackUri,
                        callbackResponse.getStatusCode());
            } catch (final ResourceAccessException resourceAccessException) {
                log.warn("Callback url '{}' is not reachable - connection timed out.", callbackUri);
            }
        } else {
            log.info("Callback for url '{}' is already sent.", callbackUri);
        }
    }

    private void addJobToSentCallbacks(final String key) {
        final LocalDateTime currentTime = LocalDateTime.now();
        synchronized (completedCallbacks) {
            completedCallbacks.put(key, currentTime.toEpochSecond(ZoneOffset.UTC));
        }

    }

    private void cleanupValuesOlderThan(final Duration otherDuration) {
        final LocalDateTime currentTime = LocalDateTime.now();
        synchronized (completedCallbacks) {
            completedCallbacks.entrySet()
                              .removeIf(entry ->
                                      Duration.between(LocalDateTime.ofEpochSecond(entry.getValue(), 0, ZoneOffset.UTC),
                                              currentTime).compareTo(otherDuration) > 0);
        }
    }

    private boolean callbackNotSentYet(final String key) {
        synchronized (completedCallbacks) {
            return !completedCallbacks.containsKey(key);
        }
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private URI buildCallbackUri(final String callbackUrl, final String jobId, final JobState jobState) {
        final Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("id", jobId);
        uriVariables.put("state", jobState);

        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(callbackUrl);
        uriComponentsBuilder.uriVariables(uriVariables);
        return uriComponentsBuilder.build().toUri();
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private URI buildCallbackUri(final String callbackUrl, final UUID orderId, final UUID batchId,
            final ProcessingState orderState, final ProcessingState batchState) {
        final Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("orderId", orderId);
        uriVariables.put("batchId", batchId);
        uriVariables.put("orderState", orderState);
        uriVariables.put("batchState", batchState);

        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(callbackUrl);
        uriComponentsBuilder.uriVariables(uriVariables);
        return uriComponentsBuilder.build().toUri();
    }
}
