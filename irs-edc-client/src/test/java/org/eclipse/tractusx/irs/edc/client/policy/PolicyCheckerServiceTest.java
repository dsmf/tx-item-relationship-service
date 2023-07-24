/********************************************************************************
 * Copyright (c) 2021,2022,2023
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 *       2022,2023: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022,2023: BOSCH AG
 * Copyright (c) 2021,2022,2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.irs.edc.client.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyCheckerServiceTest {

    private PolicyCheckerService policyCheckerService;
    @Mock
    private AcceptedPoliciesProvider policyStore;

    private static Permission createUsePermission(final Constraint constraint) {
        return Permission.Builder.newInstance()
                                 .action(Action.Builder.newInstance().type("USE").build())
                                 .constraint(constraint)
                                 .build();
    }

    private static AtomicConstraint createAtomicConstraint(final String leftExpr, final String rightExpr) {
        return AtomicConstraint.Builder.newInstance()
                                       .leftExpression(new LiteralExpression(leftExpr))
                                       .rightExpression(new LiteralExpression(rightExpr))
                                       .operator(Operator.EQ)
                                       .build();
    }

    private static Policy createAtomicConstraintPolicy(final String leftExpr, final String rightExpr) {
        final AtomicConstraint constraint = createAtomicConstraint(leftExpr, rightExpr);
        final Permission permission = createUsePermission(constraint);
        return Policy.Builder.newInstance().permission(permission).build();
    }

    private static Policy createAndConstraintPolicy(final List<Constraint> constraints) {
        final AndConstraint andConstraint = AndConstraint.Builder.newInstance()
                                                                 .constraints(constraints)
                                                                 .build();
        final Permission permission = createUsePermission(andConstraint);
        return Policy.Builder.newInstance().permission(permission).build();
    }

    private static Policy createOrConstraintPolicy(final List<Constraint> constraints) {
        final OrConstraint orConstraint = OrConstraint.Builder.newInstance()
                                                              .constraints(constraints)
                                                              .build();
        final Permission permission = createUsePermission(orConstraint);
        return Policy.Builder.newInstance().permission(permission).build();
    }

    private static Policy createXOneConstraintPolicy(final List<Constraint> constraints) {
        final XoneConstraint orConstraint = XoneConstraint.Builder.newInstance().constraints(constraints).build();
        final Permission permission = createUsePermission(orConstraint);
        return Policy.Builder.newInstance().permission(permission).build();
    }

    @BeforeEach
    void setUp() {
        final var policyList = List.of(new AcceptedPolicy("ID 3.0 Trace", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("FrameworkAgreement.traceability", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        policyCheckerService = new PolicyCheckerService(policyStore);
    }

    @ParameterizedTest
    @CsvSource(value = { "PURPOSE,ID 3.0 Trace",
                         "PURPOSE,ID%203.0%20Trace",
                         "FrameworkAgreement.traceability,active"
    }, delimiter = ',')
    void shouldConfirmValidPolicy(final String leftExpr, final String rightExpr) {
        // given
        Policy policy = createAtomicConstraintPolicy(leftExpr, rightExpr);
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectWrongPolicy() {
        // given
        Policy policy = createAtomicConstraintPolicy("idsc:PURPOSE", "Wrong_Trace");
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldRejectWhenPolicyStoreIsEmpty() {
        // given
        Policy policy = createAtomicConstraintPolicy("idsc:PURPOSE", "ID 3.0 Trace");
        when(policyStore.getAcceptedPolicies()).thenReturn(List.of());
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldConfirmValidPolicyWhenWildcardIsSet() {
        // given
        final var policyList = List.of(new AcceptedPolicy("ID 3.0 Trace", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("*", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        Policy policy = createAtomicConstraintPolicy("FrameworkAgreement.traceability", "active");
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectWhenWildcardIsPartOfPolicy() {
        // given
        final var policyList = List.of(new AcceptedPolicy("Policy*", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        Policy policy = createAtomicConstraintPolicy("FrameworkAgreement.traceability", "active");
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldValidateAndConstraints() {
        // given
        final var policyList = List.of(
                new AcceptedPolicy("FrameworkAgreement.traceability", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("Membership", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("ID 3.1 Trace", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        Policy policy = createAndConstraintPolicy(
                List.of(createAtomicConstraint("FrameworkAgreement.traceability", "active"),
                        createAtomicConstraint("Membership", "active"),
                        createAtomicConstraint("PURPOSE", "ID 3.1 Trace")));
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectAndConstraintsWhenOnlyOneMatch() {
        // given
        final var policyList = List.of(
                new AcceptedPolicy("FrameworkAgreement.traceability", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("FrameworkAgreement.dismantler", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        Policy policy = createAndConstraintPolicy(
                List.of(createAtomicConstraint("FrameworkAgreement.traceability", "active"),
                        createAtomicConstraint("Membership", "active")));
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldValidateOrConstraints() {
        // given
        final var policyList = List.of(
                new AcceptedPolicy("FrameworkAgreement.traceability", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("FrameworkAgreement.dismantler", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        Policy policy = createOrConstraintPolicy(
                List.of(createAtomicConstraint("FrameworkAgreement.traceability", "active"),
                        createAtomicConstraint("Membership", "active")));
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectOrConstraintsWhenNoneMatch() {
        // given
        final var policyList = List.of(new AcceptedPolicy("FrameworkAgreement.test", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("FrameworkAgreement.dismantler", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        Policy policy = createAndConstraintPolicy(
                List.of(createAtomicConstraint("FrameworkAgreement.traceability", "active"),
                        createAtomicConstraint("Membership", "active")));
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldValidateXOneConstraints() {
        // given
        final var policyList = List.of(
                new AcceptedPolicy("FrameworkAgreement.traceability", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("FrameworkAgreement.dismantler", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);

        Policy policy = createXOneConstraintPolicy(
                List.of(createAtomicConstraint("FrameworkAgreement.traceability", "active"),
                        createAtomicConstraint("Membership", "active")));
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectXOneConstraintsWhenNoneMatch() {
        // given
        final var policyList = List.of(new AcceptedPolicy("FrameworkAgreement.test", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("FrameworkAgreement.dismantler", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        Policy policy = createXOneConstraintPolicy(
                List.of(createAtomicConstraint("FrameworkAgreement.traceability", "active"),
                        createAtomicConstraint("Membership", "active")));
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldRejectXOneConstraintsWhenMoreThanOneMatch() {
        // given
        final var policyList = List.of(new AcceptedPolicy("FrameworkAgreement.traceability", OffsetDateTime.now().plusYears(1)),
                new AcceptedPolicy("Membership", OffsetDateTime.now().plusYears(1)));
        when(policyStore.getAcceptedPolicies()).thenReturn(policyList);
        Policy policy = createXOneConstraintPolicy(
                List.of(createAtomicConstraint("FrameworkAgreement.traceability", "active"),
                        createAtomicConstraint("Membership", "active")));
        // when
        boolean result = policyCheckerService.isValid(policy);

        // then
        assertThat(result).isFalse();
    }
}