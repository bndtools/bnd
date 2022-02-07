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

import static org.eclipse.jdt.core.IPackageFragmentRoot.K_BINARY;
import static org.eclipse.jdt.core.IPackageFragmentRoot.K_SOURCE;

import org.eclipse.jdt.core.IClasspathEntry;

public class AbstractICPEContentKindAssert<SELF extends AbstractICPEContentKindAssert<SELF, ACTUAL>, ACTUAL extends IClasspathEntry>
	extends AbstractIClasspathEntryAssert<SELF, ACTUAL> {
	AbstractICPEContentKindAssert(ACTUAL actual, Class<SELF> self) {
		super(actual, self);
	}

	public static String contentKind(int contentKind) {
		switch (contentKind) {
			case K_SOURCE :
				return "K_SOURCE (" + K_SOURCE + ")";
			case K_BINARY :
				return "K_BINARY (" + K_BINARY + ")";
			default :
				throw new IllegalArgumentException("Unrecognised classpath content kind: " + contentKind);
		}
	}

	public SELF hasContentKind(int expected) {
		isNotNull();
		// Do this here to trigger IAE if necessary
		final String expectedString = contentKind(expected);

		if (actual.getEntryKind() != expected) {
			final String actualString = contentKind(actual.getEntryKind());

			throw failureWithActualExpected(actualString, expectedString,
				"%nExpecting classpath%n  <%s>%nto have content kind:%n  <%s>%n but was:%n  <%s>", actual,
				expectedString,
				actualString);
		}
		return myself;
	}
}
