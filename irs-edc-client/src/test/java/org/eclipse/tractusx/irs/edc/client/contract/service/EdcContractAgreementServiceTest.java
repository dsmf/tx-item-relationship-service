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
package org.eclipse.tractusx.irs.edc.client.contract.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.irs.edc.client.EdcConfiguration;
import org.eclipse.tractusx.irs.edc.client.contract.model.EdcContractAgreementsResponse;
import org.eclipse.tractusx.irs.edc.client.contract.model.exception.ContractAgreementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class EdcContractAgreementServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Spy
    private EdcConfiguration edcConfiguration;

    private EdcContractAgreementService edcContractAgreementService;

    @BeforeEach
    void setUp() {
        edcConfiguration.getControlplane().setEndpoint(new EdcConfiguration.ControlplaneConfig.EndpointConfig());
        edcConfiguration.getControlplane()
                        .getEndpoint()
                        .setData("https://irs-consumer-controlplane.dev.demo.net/data/management");
        edcConfiguration.getControlplane().getEndpoint().setContractAgreements("/v2/contractagreements");
        this.edcContractAgreementService = new EdcContractAgreementService(edcConfiguration, restTemplate);
    }

    @Test
    void shouldReturnContractAgreements() throws ContractAgreementException {
        //GIVEN
        String[] contractAgreementIds = { "contractAgreementId" };

        final ContractAgreement contractAgreement = ContractAgreement.Builder.newInstance()
                                                                             .id("id")
                                                                             .assetId("assetId")
                                                                             .consumerId("consumerId")
                                                                             .providerId("providerId")
                                                                             .policy(Policy.Builder.newInstance()
                                                                                                   .build())
                                                                             .build();
        final EdcContractAgreementsResponse edcContractAgreementsResponse = EdcContractAgreementsResponse.builder()
                                                                                                         .contractAgreementList(
                                                                                                                 List.of(contractAgreement))
                                                                                                         .build();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(),
                eq(EdcContractAgreementsResponse.class))).thenReturn(ResponseEntity.ok(edcContractAgreementsResponse));

        //WHEN
        final List<ContractAgreement> contractAgreements = edcContractAgreementService.getContractAgreements(
                contractAgreementIds);

        //THEN
        Mockito.verify(restTemplate)
               .exchange(
                       eq("https://irs-consumer-controlplane.dev.demo.net/data/management/v2/contractagreements/request"),
                       any(), any(), eq(EdcContractAgreementsResponse.class));
        assertNotNull(contractAgreements);
    }

    @Test
    void shouldThrowContractAgreementExceptionWhenResponseBodyIsEmtpy() {
        //GIVEN
        String[] contractAgreementIds = { "contractAgreementId" };

        when(restTemplate.exchange(anyString(), any(), any(), eq(EdcContractAgreementsResponse.class))).thenReturn(
                ResponseEntity.ok().build());

        //WHEN
        final ContractAgreementException contractAgreementException = assertThrows(ContractAgreementException.class,
                () -> edcContractAgreementService.getContractAgreements(contractAgreementIds));

        //THEN
        Mockito.verify(restTemplate)
               .exchange(
                       eq("https://irs-consumer-controlplane.dev.demo.net/data/management/v2/contractagreements/request"),
                       any(), any(), eq(EdcContractAgreementsResponse.class));
        assertEquals("Empty message body on edc response: <200 OK OK,[]>", contractAgreementException.getMessage());
    }

    @Test
    void shouldReturnContractAgreementNegotiation() {
        //GIVEN
        String contractAgreementId = "contractAgreementId";

        final ContractNegotiation contractAgreementNegotiationMock = ContractNegotiation.Builder.newInstance()
                                                                                                .id("id")
                                                                                                .counterPartyId("")
                                                                                                .counterPartyAddress("")
                                                                                                .protocol("")
                                                                                                .build();
        when(restTemplate.exchange(anyString(), any(), any(), eq(ContractNegotiation.class))).thenReturn(
                ResponseEntity.ok(contractAgreementNegotiationMock));

        //WHEN
        final ContractNegotiation contractAgreementNegotiation = edcContractAgreementService.getContractAgreementNegotiation(
                contractAgreementId);

        //THEN
        Mockito.verify(restTemplate)
               .exchange(
                       eq("https://irs-consumer-controlplane.dev.demo.net/data/management/v2/contractagreements/contractAgreementId/negotiation"),
                       any(), any(), eq(ContractNegotiation.class));
        assertNotNull(contractAgreementNegotiation);
    }
}