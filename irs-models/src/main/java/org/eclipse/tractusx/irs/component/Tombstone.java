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

    /**
     * Search for the root cause messages including root causes of suppressed exceptions.
     * Removes duplicate messages.
     *
     * @param throwables one or more {@link Throwable}s
     * @return the distinct list of root cause messages
     */
    public static List<String> getRootErrorMessages(final Throwable... throwables) {
        return Arrays.stream(throwables)
                     .flatMap((Throwable throwable) -> getRootCauses(throwable).stream())
                     .map(t -> "%s: %s".formatted(t.getClass().getSimpleName(), t.getMessage()))
                     .distinct()
                     .toList();
    }

    /**
     * Search for the root causes including root causes of suppressed exceptions.
     *
     * @param throwable the exception with a nested or suppressed exception
     * @return the root causes
     */
    private static List<Throwable> getRootCauses(final Throwable throwable) {
        final List<Throwable> result = new ArrayList<>();

        // root cause from exception hierarchy
        result.add(ExceptionUtils.getRootCause(throwable));

        // root causes of all suppressed exceptions in the hierarchy
        Throwable currentThrowable = throwable;
        for (final Throwable suppressed : currentThrowable.getSuppressed()) {
            result.add(ExceptionUtils.getRootCause(suppressed));
        }
        while (currentThrowable.getCause() != null) {
            currentThrowable = currentThrowable.getCause();
            for (final Throwable suppressed : currentThrowable.getSuppressed()) {
                result.add(ExceptionUtils.getRootCause(suppressed));
            }
        }

        return result;
    }

}
