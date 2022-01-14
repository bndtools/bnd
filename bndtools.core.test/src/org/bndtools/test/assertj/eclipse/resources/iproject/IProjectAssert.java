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

package org.bndtools.test.assertj.eclipse.resources.iproject;

import org.assertj.core.api.InstanceOfAssertFactory;
import org.eclipse.core.resources.IProject;

public class IProjectAssert extends AbstractIProjectAssert<IProjectAssert, IProject> {

	public static final InstanceOfAssertFactory<IProject, IProjectAssert> IPROJECT = new InstanceOfAssertFactory<>(
		IProject.class, IProjectAssert::assertThat);

	public IProjectAssert(IProject actual) {
		super(actual, IProjectAssert.class);
	}

	public static IProjectAssert assertThat(IProject actual) {
		return new IProjectAssert(actual);
	}
}
