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

package org.bndtools.test.assertj.eclipse.jdt.core.compiler.imarker;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.bndtools.test.assertj.eclipse.jdt.core.compiler.iproblem.IProblemMap;
import org.bndtools.test.assertj.eclipse.resources.imarker.AbstractIMarkerAssert;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaModelMarker;

public abstract class AbstractJavaProblemMarkerAssert<SELF extends AbstractJavaProblemMarkerAssert<SELF, ACTUAL>, ACTUAL extends IMarker>
	extends AbstractIMarkerAssert<SELF, ACTUAL> {

	protected AbstractJavaProblemMarkerAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	@Override
	public SELF isCorrectType() {
		isNotNull();
		return isSubtypeOf(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
	}

	public SELF hasProblemID(int expectedID) {
		isNotNull();
		int actualID = getIntAttribute(IJavaModelMarker.ID);
		if (expectedID != actualID) {
			final String expectedDesc = IProblemMap.getProblemDescription(expectedID);
			final String actualDesc = IProblemMap.getProblemDescription(actualID);
			throw failureWithActualExpected(actualDesc, expectedDesc,
				"%nExpecting marker%n  <%s>%nto have problem ID:%n  <%s>%n but was:%n  <%s>", actual, expectedDesc,
				actualDesc);
		}
		return myself;
	}

	public SELF hasOneOfIDs(int... expectedIDs) {
		isNotNull();
		final int actualID = getIntAttribute(IJavaModelMarker.ID);
		for (int expectedID : expectedIDs) {
			if (expectedID == actualID) {
				return myself;
			}
		}
		final String expectedDesc = Arrays.stream(expectedIDs)
			.mapToObj(IProblemMap::getProblemDescription)
			.collect(Collectors.joining(",\n   "));
		final String actualDesc = IProblemMap.getProblemDescription(actualID);
		throw failure("%nExpecting marker%n  <%s>%nto have one of the problem IDs:%n  <%s>%n but it was:%n  <%s>",
			actual, expectedDesc, actualDesc);
	}

	// @Override
	// public SELF isError() {
	// isNotNull();
	// if (!actual.isError()) {
	// throw failure("%nExpecting problem%n <%s>%nto be an error, but it was
	// not", actual);
	// }
	// return myself;
	// }
	//
	// @Override
	// public SELF isWarning() {
	// isNotNull();
	// if (!actual.isWarning()) {
	// throw failure("%nExpecting problem%n <%s>%nto be a warning, but it was
	// not", actual);
	// }
	// return myself;
	// }
	//
	// @Override
	// public SELF isInfo() {
	// isNotNull();
	// if (!actual.isInfo()) {
	// throw failure("%nExpecting problem%n <%s>%nto be informational, but it
	// was not", actual);
	// }
	// return myself;
	// }
	//
	// public SELF isOnLine(int expected) {
	// isNotNull();
	// final int lineNumber = getIntAttribute("lineNumber");
	// if (lineNumber != expected) {
	// throw failureWithActualExpected(lineNumber, expected,
	// "%nExpecting problem%n <%s>%nto be on line:%n <%d>%n but was on line:%n
	// <%d>", actual, expected,
	// lineNumber);
	// }
	// return myself;
	// }
}
