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
package org.eclipse.tractusx.irs.policystore.models;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import org.eclipse.tractusx.irs.edc.client.policy.Policy;

/**
 * Policy representation for get policy by id response
 */
@Builder
public record GetPolicyByIdResponse(OffsetDateTime validUntil, Payload payload, List<String> bpn) {

    public static GetPolicyByIdResponse from(final Policy policy, final List<String> bpn) {
        return GetPolicyByIdResponse.builder()
                                    .validUntil(policy.getValidUntil())
                                    .payload(Payload.builder()
                                                    .policyId(policy.getPolicyId())
                                                    .context(Context.getDefault())
                                                    .policy(policy)
                                                    .build())
                                    .bpn(bpn)
                                    .build();
    }

}
