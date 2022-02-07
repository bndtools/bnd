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

import static org.eclipse.jdt.core.IClasspathEntry.CPE_SOURCE;

import org.eclipse.jdt.core.IClasspathEntry;

public class ICPESourceAssert extends AbstractICPEContentKindAssert<ICPESourceAssert, IClasspathEntry> {
	ICPESourceAssert(IClasspathEntry actual) {
		super(actual, ICPESourceAssert.class);
	}

	static ICPESourceAssert assertThat(IClasspathEntry actual) {
		return new ICPESourceAssert(actual);
	}

	@Override
	protected ICPESourceAssert isCorrectType() {
		return isNotNull().hasEntryKind(CPE_SOURCE);
	}
}
