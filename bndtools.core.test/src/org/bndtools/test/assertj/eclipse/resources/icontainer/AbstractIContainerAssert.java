/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.bndtools.test.assertj.eclipse.resources.icontainer;

import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.osgi.test.common.exceptions.FunctionWithException.asFunction;

import java.util.Arrays;

import org.assertj.core.api.ListAssert;
import org.bndtools.test.assertj.eclipse.resources.iresource.AbstractIResourceAssert;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

public abstract class AbstractIContainerAssert<SELF extends AbstractIContainerAssert<SELF, ACTUAL>, ACTUAL extends IContainer>
	extends AbstractIResourceAssert<SELF, ACTUAL> {

	protected AbstractIContainerAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public ListAssert<IResource> hasMembersThat() {
		return isNotNull().extracting(asFunction(actual -> Arrays.asList(actual.members())), list(IResource.class))
			.as(actual + ".members");
	}
}
