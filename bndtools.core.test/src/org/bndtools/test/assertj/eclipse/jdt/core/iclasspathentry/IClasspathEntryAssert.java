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

public class IClasspathEntryAssert extends AbstractIClasspathEntryAssert<IClasspathEntryAssert, IClasspathEntry> {

	public static final InstanceOfAssertFactory<IClasspathEntry, IClasspathEntryAssert>	CLASSPATH_ENTRY		= new InstanceOfAssertFactory<>(
		IClasspathEntry.class, IClasspathEntryAssert::assertThat);

	public static final InstanceOfAssertFactory<IClasspathEntry, ICPEProjectAssert>		CPE_PROJECT_SOURCE	= new InstanceOfAssertFactory<>(
		IClasspathEntry.class, ICPEProjectAssert::assertThat);

	public static final InstanceOfAssertFactory<IClasspathEntry, ICPESourceAssert>		CPE_SOURCE_ENTRY	= new InstanceOfAssertFactory<>(
		IClasspathEntry.class, ICPESourceAssert::assertThat);

	public IClasspathEntryAssert(IClasspathEntry actual) {
		super(actual, IClasspathEntryAssert.class);
	}

	public static IClasspathEntryAssert assertThat(IClasspathEntry actual) {
		return new IClasspathEntryAssert(actual);
	}
}
