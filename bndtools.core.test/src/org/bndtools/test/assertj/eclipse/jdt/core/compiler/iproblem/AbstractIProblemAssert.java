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

package org.bndtools.test.assertj.eclipse.jdt.core.compiler.iproblem;

import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectArrayAssert;
import org.eclipse.jdt.core.compiler.IProblem;

public abstract class AbstractIProblemAssert<SELF extends AbstractIProblemAssert<SELF, ACTUAL>, ACTUAL extends IProblem>
	extends AbstractAssert<SELF, ACTUAL> {

	protected AbstractIProblemAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public ObjectArrayAssert<String> hasArgumentsThat() {
		return isNotNull().extracting(IProblem::getArguments, InstanceOfAssertFactories.array(String[].class))
			.as(actual + ".arguments");
	}

	public SELF hasID(int expectedID) {
		isNotNull();
		if (expectedID != actual.getID()) {
			final String expectedDesc = IProblemMap.getProblemDescription(expectedID);
			final String actualDesc = IProblemMap.getProblemDescription(actual.getID());
			throw failureWithActualExpected(actual.getID(), expectedID,
				"%nExpecting problem%n  <%s>%nto have problem ID:%n  <%s>%n but was:%n  <%s>", actual, expectedDesc,
				actualDesc);
		}
		return myself;
	}

	public SELF hasOneOfIDs(int... expectedIDs) {
		isNotNull();
		for (int expectedID : expectedIDs) {
			if (expectedID == actual.getID()) {
				return myself;
			}
		}
		final String expectedDesc = Arrays.stream(expectedIDs)
			.mapToObj(IProblemMap::getProblemDescription)
			.collect(Collectors.joining(",\n   "));
		final String actualDesc = IProblemMap.getProblemDescription(actual.getID());
		throw failure("%nExpecting problem%n  <%s>%nto have one of the problem IDs:%n  <%s>%n but it was:%n  <%s>",
			actual,
			expectedDesc, actualDesc);
	}

	public SELF hasMessage(String expected) {
		isNotNull();
		final String actualMessage = actual.getMessage();
		if (!Objects.equals(actualMessage, expected)) {
			throw failureWithActualExpected(actualMessage, expected,
				"%nExpecting problem%n  <%s>%nto have message:%n  <%s>%n but was:%n  <%s>", actual, expected,
				actualMessage);
		}
		return myself;
	}

	public SELF hasMessageContaining(String expected) {
		isNotNull();
		final String actualMessage = actual.getMessage();
		if (!actualMessage.contains(expected)) {
			throw failure("%nExpecting problem%n  <%s>%nto have message containing:%n  <%s>%n but was:%n  <%s>", actual,
				expected, actualMessage);
		}
		return myself;
	}

	public SELF hasMessageContainingMatch(String expected) {
		isNotNull();
		final String actualMessage = actual.getMessage();
		Pattern p = Pattern.compile(expected);
		Matcher m = p.matcher(actualMessage);
		if (!m.find()) {
			throw failure("%nExpecting problem%n  <%s>%nto have message containing match:%n  <%s>%n but was:%n  <%s>",
				actual,
				expected, actualMessage);
		}
		return myself;
	}

	public AbstractStringAssert<?> hasMessageThat() {
		return isNotNull().extracting(IProblem::getMessage, STRING)
			.as(actual + ".message");
	}

	public SELF isError() {
		isNotNull();
		if (!actual.isError()) {
			throw failure("%nExpecting problem%n  <%s>%nto be an error, but it was not", actual);
		}
		return myself;
	}

	public SELF isWarning() {
		isNotNull();
		if (!actual.isWarning()) {
			throw failure("%nExpecting problem%n  <%s>%nto be a warning, but it was not", actual);
		}
		return myself;
	}

	public SELF isInfo() {
		isNotNull();
		if (!actual.isInfo()) {
			throw failure("%nExpecting problem%n  <%s>%nto be informational, but it was not", actual);
		}
		return myself;
	}

	public SELF hasLineNumber(int expected) {
		isNotNull();
		if (actual.getSourceLineNumber() != expected) {
			throw failureWithActualExpected(actual.getSourceLineNumber(), expected,
				"%nExpecting problem%n  <%s>%nto be on line:%n  <%d>%n but was on line:%n  <%d>", actual, expected,
				actual.getSourceLineNumber());
		}
		return myself;
	}
}
