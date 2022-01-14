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

import org.assertj.core.api.InstanceOfAssertFactory;
import org.eclipse.core.resources.IContainer;

public class IContainerAssert extends AbstractIContainerAssert<IContainerAssert, IContainer> {

	public static final InstanceOfAssertFactory<IContainer, IContainerAssert> ICONTAINER = new InstanceOfAssertFactory<>(
		IContainer.class, IContainerAssert::assertThat);

	public IContainerAssert(IContainer actual) {
		super(actual, IContainerAssert.class);
	}

	public static IContainerAssert assertThat(IContainer actual) {
		return new IContainerAssert(actual);
	}
}
