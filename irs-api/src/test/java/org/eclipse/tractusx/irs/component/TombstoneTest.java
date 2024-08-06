/********************************************************************************
 * Copyright (c) 2022,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
package org.eclipse.tractusx.irs.component;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import io.github.resilience4j.retry.RetryRegistry;
import org.eclipse.tractusx.irs.component.enums.ProcessStep;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TombstoneTest {

    @Test
    void buildTombstoneTest() {
        // arrange
        final String catenaXId = "5e3e9060-ba73-4d5d-a6c8-dfd5123f4d99";
        final IllegalArgumentException exception = new IllegalArgumentException("Some funny error occur");
        final String endPointUrl = "http://localhost/dummy/interfaceinformation/urn:uuid:8a61c8db-561e-4db0-84ec-a693fc5ffdf6";

        final ProcessingError processingError = ProcessingError.builder()
                                                               .withProcessStep(ProcessStep.SUBMODEL_REQUEST)
                                                               .withRetryCounter(RetryRegistry.ofDefaults()
                                                                                              .getDefaultConfig()
                                                                                              .getMaxAttempts())
                                                               .withLastAttempt(ZonedDateTime.now(ZoneOffset.UTC))
                                                               .withErrorDetail("Some funny error occur")
                                                               .build();

        final Tombstone expectedTombstone = Tombstone.builder()
                                                     .catenaXId(catenaXId)
                                                     .endpointURL(endPointUrl)
                                                     .processingError(processingError)
                                                     .build();

        // act
        final int retryCount = RetryRegistry.ofDefaults().getDefaultConfig().getMaxAttempts();
        final ProcessingError error = ProcessingError.builder()
                                                     .withProcessStep(ProcessStep.SUBMODEL_REQUEST)
                                                     .withRetryCounterAndLastAttemptNow(retryCount)
                                                     .withErrorDetail(exception.getMessage())
                                                     .build();
        final Tombstone tombstone = Tombstone.builder()
                                             .endpointURL(endPointUrl)
                                             .catenaXId(catenaXId)
                                             .processingError(error)
                                             .build();

        // assert
        assertThat(tombstone).isNotNull();
        assertThat(tombstone.getProcessingError().getErrorDetail()).isEqualTo(processingError.getErrorDetail());
        assertThat(tombstone.getProcessingError().getRetryCounter()).isEqualTo(processingError.getRetryCounter());
        assertThat(zonedDateTimeExcerpt(tombstone.getProcessingError().getLastAttempt())).isEqualTo(
                zonedDateTimeExcerpt(processingError.getLastAttempt()));
        assertThat(tombstone.getCatenaXId()).isEqualTo(expectedTombstone.getCatenaXId());
        assertThat(tombstone.getEndpointURL()).isEqualTo(expectedTombstone.getEndpointURL());
        assertThat(tombstone.getProcessingError().getRetryCounter()).isEqualTo(
                expectedTombstone.getProcessingError().getRetryCounter());
    }

    @Test
    void shouldUseSuppressedExceptionWhenPresent() {
        // arrange
        final String mainExceptionMessage = "Exception occurred.";
        final Exception exception = new Exception(mainExceptionMessage);
        final String suppressedExceptionMessage = "Suppressed Exception which occurred deeper.";
        exception.addSuppressed(new Exception(suppressedExceptionMessage));
        final Throwable[] suppressed = exception.getSuppressed();

        // act

        final ProcessingError error = ProcessingError.builder()
                                                     .withProcessStep(ProcessStep.DIGITAL_TWIN_REQUEST)
                                                     .withRetryCounterAndLastAttemptNow(1)
                                                     .withErrorDetail(exception.getMessage())
                                                     .withRootCauses(Tombstone.getRootErrorMessages(suppressed))
                                                     .build();
        final Tombstone tombstone = Tombstone.builder()
                                             .endpointURL("testUrl")
                                             .catenaXId("testId")
                                             .processingError(error)
                                             .build();

        // assert
        assertThat(tombstone.getProcessingError().getErrorDetail()).isEqualTo(exception.getMessage());
        assertThat(tombstone.getProcessingError().getRootCauses()).contains("Exception: " + suppressedExceptionMessage);
    }

    @Test
    void shouldUseDeepSuppressedExceptionWhenPresent() {
        // arrange
        final Exception exception = new Exception("Exception occurred.");

        final Exception rootCause = new Exception("Wrapper exception to the root cause");
        final String suppressedRootCause = "Root cause of the exception";
        rootCause.addSuppressed(new Exception(suppressedRootCause));

        final Exception suppressedWrapperException = new Exception(
                "Suppressed Exception which was added through Futures.", rootCause);
        exception.addSuppressed(suppressedWrapperException);

        final Throwable[] suppressed = exception.getSuppressed();

        // act
        final ProcessingError error = ProcessingError.builder()
                                                     .withProcessStep(ProcessStep.DIGITAL_TWIN_REQUEST)
                                                     .withRetryCounterAndLastAttemptNow(1)
                                                     .withErrorDetail(exception.getMessage())
                                                     .withRootCauses(Tombstone.getRootErrorMessages(suppressed))
                                                     .build();
        final Tombstone tombstone = Tombstone.builder()
                                             .endpointURL("testUrl")
                                             .catenaXId("testId")
                                             .processingError(error)
                                             .build();

        // assert
        assertThat(tombstone.getProcessingError().getErrorDetail()).isEqualTo(exception.getMessage());
        assertThat(tombstone.getProcessingError().getRootCauses()).contains("Exception: " + suppressedRootCause);
    }

    @Test
    void shouldUseExceptionMessageWhenSuppressedExceptionNotPresent() {
        // arrange
        final String mainExceptionMessage = "Exception occurred.";
        final Exception exception = new Exception(mainExceptionMessage);
        final Throwable[] suppressed = exception.getSuppressed();

        // act

        final ProcessingError error = ProcessingError.builder()
                                                     .withProcessStep(ProcessStep.DIGITAL_TWIN_REQUEST)
                                                     .withRetryCounterAndLastAttemptNow(1)
                                                     .withErrorDetail(exception.getMessage())
                                                     .withRootCauses(Tombstone.getRootErrorMessages(suppressed))
                                                     .build();
        final Tombstone tombstone = Tombstone.builder()
                                             .endpointURL("testUrl")
                                             .catenaXId("testId")
                                             .processingError(error)
                                             .build();

        // assert
        assertThat(tombstone.getProcessingError().getErrorDetail()).isEqualTo(exception.getMessage());
        assertThat(tombstone.getProcessingError().getRootCauses()).isEmpty();
    }

    private String zonedDateTimeExcerpt(ZonedDateTime dateTime) {
        return "%d-%s-%dT%d:%d:%d".formatted(dateTime.getYear(), dateTime.getMonth(), dateTime.getDayOfMonth(),
                dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }

    @Nested
    class GetRootErrorMessagesTests {

        @Test
        public void testSingleThrowable() {
            final Throwable t = new Throwable("Root cause message");
            final List<String> messages = Tombstone.getRootErrorMessages(t);
            assertThat(messages).containsExactly("Throwable: Root cause message");
        }

        @Test
        public void testMultipleThrowables() {
            final Throwable t1 = new Throwable("Root cause message 1");
            final Throwable t2 = new Throwable("Root cause message 2");

            final List<String> messages = Tombstone.getRootErrorMessages(t1, t2);

            assertThat(messages).containsExactlyInAnyOrder("Throwable: Root cause message 1",
                    "Throwable: Root cause message 2");
        }

        @Test
        public void testNestedThrowable() {
            final Throwable rootCause = new Throwable("Root cause message");
            final Throwable exception = new Throwable("My exception", rootCause);

            final List<String> messages = Tombstone.getRootErrorMessages(exception);

            assertThat(messages).containsExactly("Throwable: Root cause message");
        }

        @Test
        public void testSuppressedThrowable() {
            final Throwable suppressed = new Throwable("Suppressed message");
            final Throwable main = new Throwable("Main exception");
            main.addSuppressed(suppressed);

            final List<String> messages = Tombstone.getRootErrorMessages(main);

            assertThat(messages).containsExactlyInAnyOrder("Throwable: Main exception",
                    "Throwable: Suppressed message");
        }

        @Test
        public void testDistinctMessages() {
            final Throwable t1 = new Throwable("Same message");
            final Throwable t2 = new Throwable("Same message");

            final List<String> messages = Tombstone.getRootErrorMessages(t1, t2);

            assertThat(messages).containsExactly("Throwable: Same message");
        }

        @Test
        public void testComplexExceptionHierarchy() {
            final Throwable rootCause = new Throwable("Root cause message");
            final Throwable rootCauseFromSuppressed1 = new Throwable("Root cause message from suppressed 1");
            final Throwable rootCauseFromSuppressed2 = new Throwable("Root cause message from suppressed 2");
            final Throwable suppressed1 = new Throwable("Suppressed message 1", rootCauseFromSuppressed1);
            final Throwable suppressed2 = new Throwable("Suppressed message 2", rootCauseFromSuppressed2);
            final Throwable exception = new Throwable("My exception", rootCause);
            exception.addSuppressed(suppressed1);
            exception.addSuppressed(suppressed2);

            final List<String> messages = Tombstone.getRootErrorMessages(exception);

            assertThat(messages).containsExactlyInAnyOrder("Throwable: Root cause message",
                    "Throwable: Root cause message from suppressed 1",
                    "Throwable: Root cause message from suppressed 2");
        }

        @Test
        public void testComplexExceptionHierarchy_duplicateRootCauseFromSuppressedExceptions() {
            final Throwable rootCause = new Throwable("Root cause message");
            final Throwable rootCauseFromSuppressed1 = new Throwable("Root cause message from suppressed");
            final Throwable rootCauseFromSuppressed2 = new Throwable("Root cause message from suppressed");
            final Throwable suppressed1 = new Throwable("Suppressed message", rootCauseFromSuppressed1);
            final Throwable suppressed2 = new Throwable("Suppressed message 2", rootCauseFromSuppressed2);
            final Throwable exception = new Throwable("My exception", rootCause);
            exception.addSuppressed(suppressed1);
            exception.addSuppressed(suppressed2);

            final List<String> messages = Tombstone.getRootErrorMessages(exception);

            assertThat(messages).containsExactlyInAnyOrder("Throwable: Root cause message",
                    "Throwable: Root cause message from suppressed");
        }

        @Test
        public void testEmptyInput() {
            final List<String> messages = Tombstone.getRootErrorMessages();
            assertThat(messages).isEmpty();
        }
    }

}
