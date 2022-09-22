/********************************************************************************
 * Copyright (c) 2021,2022
 *       2022: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0. *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.esr.supplyon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.tractusx.esr.controller.model.CertificateType;
import org.junit.jupiter.api.Test;

class SupplyOnFacadeTest {

    private final SupplyOnClient supplyOnClient = mock(SupplyOnClient.class);
    private final SupplyOnFacade supplyOnFacade = new SupplyOnFacade(supplyOnClient);

    @Test
    void shouldReturnEsrCertificateData() {
        final String requestorBPN = "BPNL00000003AYRE";
        final String supplierBPN = "BPNL00000003XXX";
        final String certificateName = CertificateType.ISO14001.name();
        when(supplyOnClient.getESRCertificate(requestorBPN, supplierBPN, certificateName))
                .thenReturn(EsrCertificate.builder().certificateState(CertificateState.VALID).build());

        final EsrCertificate esrCertificate = supplyOnFacade.getESRCertificate(requestorBPN, supplierBPN, CertificateType.ISO14001);

        assertThat(esrCertificate).isNotNull();
        assertThat(esrCertificate.getCertificateState()).isNotNull();
    }

}
