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

package org.bndtools.test.assertj.eclipse.jdt.core.iclasspathentry;

import org.assertj.core.api.InstanceOfAssertFactory;
import org.eclipse.jdt.core.IClasspathEntry;

public class ICPEProjectAssert extends AbstractICPEContentKindAssert<ICPEProjectAssert, IClasspathEntry> {
	ICPEProjectAssert(IClasspathEntry actual) {
		super(actual, ICPEProjectAssert.class);
	}

	public static final InstanceOfAssertFactory<IClasspathEntry, ICPEProjectAssert> CPE_PROJECT = new InstanceOfAssertFactory<>(
		IClasspathEntry.class, ICPEProjectAssert::assertThat);

	static ICPEProjectAssert assertThat(IClasspathEntry actual) {
		return new ICPEProjectAssert(actual);
	}

	public ICPEProjectAssert combinesAccessRules() {
		isNotNull();
		if (!actual.combineAccessRules()) {
			throw failure("%nExpecting classpath%n  <%s>%nto combine access rules, but it does not", actual);
		}
		return myself;
	}

	public ICPEProjectAssert doesNotCombineAccessRules() {
		isNotNull();
		if (actual.combineAccessRules()) {
			throw failure("%nExpecting classpath%n  <%s>%nto not combine access rules, but it does", actual);
		}
		return myself;
	}

	@Override
	protected ICPEProjectAssert isCorrectType() {
		return isNotNull().hasEntryKind(IClasspathEntry.CPE_PROJECT);
	}
}
