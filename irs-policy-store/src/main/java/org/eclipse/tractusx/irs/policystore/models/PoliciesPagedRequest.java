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
package org.eclipse.tractusx.irs.policystore.models;

import static org.eclipse.tractusx.irs.policystore.common.CommonConstants.PARAM_BUSINESS_PARTNER_NUMBERS;
import static org.eclipse.tractusx.irs.policystore.common.CommonConstants.PARAM_SEARCH;
import static org.eclipse.tractusx.irs.policystore.common.CommonConstants.PROPERTY_BPN;
import static org.eclipse.tractusx.irs.policystore.controllers.PolicyStoreController.DEFAULT_PAGE_SIZE;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.tractusx.irs.policystore.validators.ValidListOfBusinessPartnerNumbers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;

/**
 * Object for API to create policy
 */
@SuppressWarnings("FileTabCharacter")
@Schema(description = "Request to query policies")
@NoArgsConstructor
@Data
@Validated
public class PoliciesPagedRequest {

    public static final int SORT_PARTS = 2;

    @Schema(description = "Page number")
    private int page;

    @Schema(description = "Page size")
    private int size = DEFAULT_PAGE_SIZE;

    @Schema(description = """
            Search parameters, each in the following form:
            `<property>,[EQUALS|STARTS_WITH|BEFORE_LOCAL_DATE|AFTER_LOCAL_DATE],<value>`.
            Example:
            `["BPN,STARTS_WITH,BPNL12", "policyId,STARTS_WITH,policy2", "validUntil,AFTER_LOCAL_DATE,2024-06-05"]`.
            """)
    private List<String> search;

    @Schema(description = """
            Sort parameters, each in the following form:
            `<property>,[asc|desc]`.
             Example: `["BPN,asc", "policyId,desc"]`.
            """)
    private List<String> sort;

    @ValidListOfBusinessPartnerNumbers(allowDefault = true) //
    @Schema(name = PARAM_BUSINESS_PARTNER_NUMBERS, description = "List of business partner numbers. "
            + "This may also contain the value \"default\" in order to query the default policies.") //
    private List<String> businessPartnerNumbers;

    @JsonIgnore
    public Sort parseSortParameters() {
        Sort sortObj = null;
        if (sort != null) {
            for (final String sortParam : sort) {
                final String[] parts = sortParam.split(",");
                if (parts.length == SORT_PARTS) {
                    final String property = parts[0];
                    final Sort.Direction direction = Sort.Direction.fromString(parts[1]);
                    if (sortObj == null || sortObj.isUnsorted()) {
                        sortObj = Sort.by(direction, property);
                    } else {
                        sortObj = sortObj.and(Sort.by(direction, property));
                    }
                }
            }
        }
        return sortObj != null ? sortObj : Sort.by(Sort.Direction.ASC, PROPERTY_BPN);
    }

    @JsonIgnore
    public PageRequest getPageable() {
        return PageRequest.of(getPage(), getSize(), parseSortParameters());
    }

    @JsonIgnore
    public Map<String, String[]> getParameterMap() {

        final Map<String, String[]> parameterMap = new ConcurrentHashMap<>();

        if (getBusinessPartnerNumbers() != null) {
            parameterMap.put(PARAM_BUSINESS_PARTNER_NUMBERS, getBusinessPartnerNumbers().toArray(new String[0]));
        }

        if (getSearch() != null) {
            parameterMap.put(PARAM_SEARCH, getSearch().toArray(new String[0]));
        }

        return parameterMap;
    }

}
