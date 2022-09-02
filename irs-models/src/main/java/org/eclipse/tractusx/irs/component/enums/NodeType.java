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
package org.eclipse.tractusx.irs.component.enums;

import lombok.Getter;

/**
 * Node Type Enum
 */
@Getter
public enum NodeType {
    ROOT("Root Node of the tree - the initial C-X ID"),
    NODE("Node of the tree with children - further AssemblyPartRelationShip aspects"),
    LEAF("Leaf node of the tree - No further AssemblyPartRelationShip aspects"),
    TOMBSTONE("Exceptional state - transient exception");

    private final String description;

    NodeType(final String description) {
        this.description = description;
    }
}
