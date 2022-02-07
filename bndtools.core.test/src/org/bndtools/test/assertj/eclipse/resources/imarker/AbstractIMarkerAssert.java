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

package org.bndtools.test.assertj.eclipse.resources.imarker;

import static aQute.bnd.exceptions.FunctionWithException.asFunction;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.bndtools.test.assertj.eclipse.resources.iresource.IResourceAssert.IRESOURCE;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
import org.bndtools.test.assertj.eclipse.resources.iresource.IResourceAssert;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import aQute.bnd.exceptions.Exceptions;

public abstract class AbstractIMarkerAssert<SELF extends AbstractIMarkerAssert<SELF, ACTUAL>, ACTUAL extends IMarker>
	extends AbstractAssert<SELF, ACTUAL> {

	protected AbstractIMarkerAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF exists() {
		isNotNull();
		if (!actual.exists()) {
			throw failure("%nExpecting marker%n  <%s>%nto exist, but it does not", actual);
		}
		return myself;
	}

	public SELF doesNotExist() {
		isNotNull();
		if (actual.exists()) {
			throw failure("%nExpecting marker%n  <%s>%nto not exist, but it does", actual);
		}
		return myself;
	}

	public SELF hasAttribute(String attributeName) {
		isNotNull();
		try {
			if (actual.getAttribute(attributeName) == null) {
				throw failure("%nExpecting%n <%s>%nto have attribute:%n <%s>%n but it did not", actual, attributeName);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public AbstractStringAssert<?> hasAttributeWithNameThat(String attributeName) {
		return isNotNull().extracting(asFunction(marker -> marker.getAttribute(attributeName)), STRING)
			.as(actual + ".attribute(" + attributeName + ")");
	}

	public SELF doesNotHaveAttribute(String attributeName) {
		isNotNull();
		try {
			Object value = actual.getAttribute(attributeName);
			if (value != null) {
				throw failure("%nExpecting%n  <%s>%nto not have attribute:%n  <%s>%n but it did:%n  <%s>", actual,
					attributeName, value);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public MapAssert<String, Object> hasAttributesThat() {
		return isNotNull().extracting(asFunction(IMarker::getAttributes), map(String.class, Object.class))
			.as(actual + ".attributes");
	}

	public IResourceAssert isForResourceThat() {
		return isNotNull().extracting(IMarker::getResource, IRESOURCE)
			.as(actual + ".resource");
	}

	public SELF isForResource(IResource expected) {
		isNotNull();
		if (!Objects.equals(actual.getResource(), expected)) {
			throw failureWithActualExpected(actual.getResource(), expected,
				"%nExpecting marker%n  <%s>%nto be for resource:%n  <%s>%n but it was for:%n  <%s>", actual, expected,
				actual.getResource());
		}
		return myself;
	}

	public SELF hasType(String expected) {
		isNotNull();
		try {
			if (!Objects.equals(actual.getType(), expected)) {
				throw failureWithActualExpected(actual.getType(), expected,
					"%nExpecting%n  <%s>%nto have type:%n  <%s>%n but was:%n  <%s>", actual, expected,
					actual.getType());
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public <NARROW extends AbstractIMarkerAssert<NARROW, ?>> NARROW hasType(
		InstanceOfAssertFactory<IMarker, NARROW> expected) {
		isNotNull();
		return isNotNull().asInstanceOf((InstanceOfAssertFactory<?, NARROW>) expected)
			.isCorrectType();
	}

	// This should not be exposed publicly; the public interface is via
	// hasType(InstanceOfAssertFactory)
	protected SELF isCorrectType() {
		return myself;
	}

	public AbstractStringAssert<?> hasTypeThat() {
		return isNotNull().extracting(asFunction(IMarker::getType), STRING)
			.as(actual + ".type");
	}

	public SELF isSubtypeOf(String... superTypes) {
		isNotNull();

		StringBuilder not = new StringBuilder();
		try {
			for (String st : superTypes) {
				if (!actual.isSubtypeOf(st)) {
					not.append(st)
						.append(',');
				}
			}

			if (not.length() != 0) {
				not.setLength(not.length() - 1);
				String all = Stream.of(superTypes)
					.collect(Collectors.joining(","));
				throw failure("%nExpecting%n  <%s>%nto be a subtype of:%n  <%s>%n but it was not a subtype of:%n  <%s>",
					actual, all, not);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF isNotSubtypeOf(String... superTypes) {
		isNotNull();

		StringBuilder is = new StringBuilder();
		try {
			for (String st : superTypes) {
				if (actual.isSubtypeOf(st)) {
					is.append(st)
						.append(',');
				}
			}

			if (is.length() != 0) {
				is.setLength(is.length() - 1);
				String all = Stream.of(superTypes)
					.collect(Collectors.joining(","));
				throw failure(
					"%nExpecting%n  <%s> (type <%s>)%nto not be a subtype of any of:%n  <%s>%n but it was a subtype of:%n  <%s>",
					actual, actual.getType(), all, is);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF isNotSubtypeOf(String superType) {
		isNotNull();

		try {
			if (actual.isSubtypeOf(superType)) {
				throw failure("%nExpecting%n  <%s> (type <%s>)%nto not be a subtype of:%n  <%s>%n but it was", actual,
					actual.getType(), superType);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF isSubtypeOfOneOf(String... superTypes) {
		isNotNull();

		try {
			boolean result = false;
			for (String st : superTypes) {
				if (actual.isSubtypeOf(st)) {
					result = true;
					break;
				}
			}

			if (!result) {
				String all = Stream.of(superTypes)
					.collect(Collectors.joining(","));
				throw failure(
					"%nExpecting%n  <%s> (type <%s>)%nto be a subtype of one of:%n  <%s>%n but it was not a subtype of any",
					actual, actual.getType(), all);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	// Convenience for pre-defined marker types
	public SELF isBookmark() {
		return isSubtypeOf(IMarker.BOOKMARK);
	}

	public SELF isNotBookmark() {
		return isNotSubtypeOf(IMarker.BOOKMARK);
	}

	public SELF isProblem() {
		return isSubtypeOf(IMarker.PROBLEM);
	}

	public SELF isNotProblem() {
		return isNotSubtypeOf(IMarker.PROBLEM);
	}

	public SELF isTask() {
		return isSubtypeOf(IMarker.TASK);
	}

	public SELF isNotTask() {
		return isNotSubtypeOf(IMarker.TASK);
	}

	public SELF isText() {
		return isSubtypeOf(IMarker.TEXT);
	}

	public SELF isNotText() {
		return isNotSubtypeOf(IMarker.TEXT);
	}

	public SELF hasSeverity(int severity) {
		hasAttribute(IMarker.SEVERITY);
		int actualSeverity = getIntAttribute(IMarker.SEVERITY);
		if (actualSeverity != severity) {
			throw failureWithActualExpected(actualSeverity, severity,
				"%nExpecting marker%n  <%s>%nto have severity:%n  <%d:%s>%n but it was:%n  <%d:%s>", actual, severity,
				severityToString(severity), actualSeverity, severityToString(actualSeverity));
		}
		return myself;
	}

	public SELF isError() {
		return hasSeverity(IMarker.SEVERITY_ERROR);
	}

	public SELF isWarning() {
		return hasSeverity(IMarker.SEVERITY_WARNING);
	}

	public SELF isInfo() {
		return hasSeverity(IMarker.SEVERITY_INFO);
	}

	private String severityToString(int severity) {
		switch (severity) {
			case IMarker.SEVERITY_ERROR :
				return "ERROR";
			case IMarker.SEVERITY_WARNING :
				return "WARNING";
			case IMarker.SEVERITY_INFO :
				return "INFO";
			default :
				return "UNKNOWN";
		}
	}

	public SELF hasPriority(int priority) {
		hasAttribute(IMarker.PRIORITY);
		int actualPriority = getIntAttribute(IMarker.SEVERITY);
		if (actualPriority != priority) {
			throw failureWithActualExpected(actualPriority, priority,
				"%nExpecting marker%n  <%s>%nto have priority:%n  <%d:%s>%n but it was:%n  <%d:%s>", actual, priority,
				priorityToString(priority), actualPriority, priorityToString(actualPriority));
		}
		return myself;
	}

	public SELF isHighPriority() {
		return hasPriority(IMarker.PRIORITY_HIGH);
	}

	public SELF isNormalPriority() {
		return hasPriority(IMarker.PRIORITY_NORMAL);
	}

	public SELF isLowPriority() {
		return hasPriority(IMarker.PRIORITY_LOW);
	}

	private String priorityToString(int priority) {
		switch (priority) {
			case IMarker.PRIORITY_HIGH :
				return "HIGH";
			case IMarker.PRIORITY_NORMAL :
				return "NORMAL";
			case IMarker.PRIORITY_LOW :
				return "LOW";
			default :
				return "UNKNOWN";
		}
	}

	public SELF isDone() {
		isNotNull();
		boolean done = getBoolAttribute(IMarker.DONE);
		if (!done) {
			throw failure("%nExpecting marker%n  <%s>%nto be done, but it was not", actual);
		}
		return myself;
	}

	public SELF isNotDone() {
		isNotNull();
		boolean done = getBoolAttribute(IMarker.DONE);
		if (done) {
			throw failure("%nExpecting marker%n  <%s>%nto not be done, but it was", actual);
		}
		return myself;
	}

	public SELF hasLocation(String expected) {
		isNotNull();
		String location = getStringAttribute(IMarker.LOCATION);
		if (!Objects.equals(location, expected)) {
			throw failureWithActualExpected(location, expected,
				"%nExpecting%n  <%s>%nto have location:%n  <%s>%n but was:%n  <%s>", actual, expected, location);
		}
		return myself;
	}

	public AbstractStringAssert<?> hasLocationThat() {
		return isNotNull().extracting(marker -> getStringAttribute(IMarker.LOCATION), STRING)
			.as(actual + ".location");
	}

	public SELF hasMessage(String expected) {
		String message = getStringAttribute(IMarker.MESSAGE);
		if (!Objects.equals(message, expected)) {
			throw failureWithActualExpected(message, expected,
				"%nExpecting%n  <%s>%nto have message:%n  <%s>%n but was:%n  <%s>", actual, expected, message);
		}
		return myself;
	}

	public SELF hasMessageContaining(String expected) {
		String message = getStringAttribute(IMarker.MESSAGE);
		if (!message.contains(expected)) {
			throw failure("%nExpecting%n  <%s>%nto have message containing:%n  <%s>%n but was:%n  <%s>", actual,
				expected, message);
		}
		return myself;
	}

	public SELF hasMessageContainingMatch(String expected) {
		String message = getStringAttribute(IMarker.MESSAGE);
		Pattern p = Pattern.compile(expected);
		Matcher m = p.matcher(message);
		if (!m.find()) {
			throw failure("%nExpecting%n  <%s>%nto have message containing match:%n  <%s>%n but was:%n  <%s>", actual,
				expected, message);
		}
		return myself;
	}

	public AbstractStringAssert<?> hasMessageThat() {
		return isNotNull().extracting(marker -> getStringAttribute(IMarker.MESSAGE), STRING)
			.as(actual + ".message");
	}

	public SELF hasCharStart(int expected) {
		isNotNull();
		int start = getIntAttribute(IMarker.CHAR_START);
		if (start != expected) {
			throw failureWithActualExpected(start, expected,
				"%nExpecting%n  <%s>%nto have char start:%n  <%d>%n but was:%n  <%d>", actual, expected, start);
		}
		return myself;
	}

	public AbstractIntegerAssert<?> hasCharStartThat() {
		return isNotNull().extracting(marker -> getIntAttribute(IMarker.CHAR_START), INTEGER)
			.as(actual + ".charStart");
	}

	public SELF hasCharEnd(int expected) {
		isNotNull();
		int end = getIntAttribute(IMarker.CHAR_END);
		if (end != expected) {
			throw failureWithActualExpected(end, expected,
				"%nExpecting%n  <%s>%nto have char end:%n  <%d>%n but was:%n  <%d>", actual, expected, end);
		}
		return myself;
	}

	public AbstractIntegerAssert<?> hasCharEndThat() {
		return isNotNull().extracting(marker -> getIntAttribute(IMarker.CHAR_END), INTEGER)
			.as(actual + ".charEnd");
	}

	public SELF hasLineNumber(int expected) {
		isNotNull();
		int line = getIntAttribute(IMarker.LINE_NUMBER);
		if (line != expected) {
			throw failureWithActualExpected(line, expected,
				"%nExpecting%n  <%s>%nto have line number:%n  <%d>%n but was:%n  <%d>", actual, expected, line);
		}
		return myself;
	}

	public AbstractIntegerAssert<?> hasLineNumberThat() {
		return isNotNull().extracting(marker -> getIntAttribute(IMarker.LINE_NUMBER), INTEGER)
			.as(actual + ".lineNumber");
	}

	protected boolean getBoolAttribute(String attributeName) {
		try {
			hasAttribute(attributeName);
			return (boolean) actual.getAttribute(attributeName);
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	protected int getIntAttribute(String attributeName) {
		try {
			hasAttribute(attributeName);
			return (int) actual.getAttribute(attributeName);
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	protected String getStringAttribute(String attributeName) {
		try {
			hasAttribute(attributeName);
			return (String) actual.getAttribute(attributeName);
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}
}
