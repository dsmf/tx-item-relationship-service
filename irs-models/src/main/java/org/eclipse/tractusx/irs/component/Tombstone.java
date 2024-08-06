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
package org.eclipse.tractusx.irs.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.tractusx.irs.component.enums.NodeType;

/**
 * Tombstone with information about request failure
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@Schema(description = "Tombstone with information about request failure")
@ToString
public class Tombstone {

    public static final int CATENA_X_ID_LENGTH = 45;

    private static final NodeType NODE_TYPE = NodeType.TOMBSTONE;

    @Schema(description = "CATENA-X global asset id in the format urn:uuid:uuid4.",
            example = "urn:uuid:6c311d29-5753-46d4-b32c-19b918ea93b0", minLength = CATENA_X_ID_LENGTH,
            maxLength = CATENA_X_ID_LENGTH,
            pattern = "^urn:uuid:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private final String catenaXId;
    private final String endpointURL;
    private final String businessPartnerNumber;
    private final ProcessingError processingError;
    private final Map<String, Object> policy;

    public static List<String> getRootErrorMessages(final Throwable... throwables) {
        return Arrays.stream(throwables)
                     .flatMap((Throwable throwable) -> getRootCauses_(throwable).stream())
                     .map(Throwable::getMessage)
                     .toList();
    }

    private static List<Throwable> getRootCauses_(final Throwable throwable) {
        final List<Throwable> result = new ArrayList<>();
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
            //            if (rootCause.getSuppressed() != null && rootCause.getSuppressed().length > 0) {
            //                for (int i = 0; i < rootCause.getSuppressed().length; i++) {
            //                    result.add(ExceptionUtils.getRootCause(rootCause.getSuppressed()[i]));
            //                }
            //            }

        }
        result.add(rootCause);

        rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
            if (rootCause.getSuppressed() != null && rootCause.getSuppressed().length > 0) {
                for (int i = 0; i < rootCause.getSuppressed().length; i++) {
                    result.add(ExceptionUtils.getRootCause(rootCause.getSuppressed()[i]));
                }
            }

        }

        return result;
    }

    /**
     * Search for the root cause or suppressed exception as long as there is a cause or suppressed exception.
     * Stop after a depth of 10 to prevent endless loop.
     *
     * @param throwable the exception with a nested or suppressed exception
     * @return the root cause, eiter suppressed or nested
     */
    private static String getRootErrorMessages(final Throwable throwable) {
        final Throwable cause = throwable.getCause();

        if (cause != null) {
            Throwable rootCause = cause;
            int depth = 0;
            final int maxDepth = 10;
            while ((rootCause.getCause() != null || hasSuppressedExceptions(rootCause)) && depth < maxDepth) {
                if (hasSuppressedExceptions(rootCause)) {
                    rootCause = rootCause.getSuppressed()[0];
                } else {
                    rootCause = rootCause.getCause();
                }
                depth++;
            }
            return ExceptionUtils.getRootCauseMessage(rootCause);
        }
        return ExceptionUtils.getRootCauseMessage(throwable);
    }

    private static boolean hasSuppressedExceptions(final Throwable exception) {
        return exception.getSuppressed().length > 0;
    }
}
