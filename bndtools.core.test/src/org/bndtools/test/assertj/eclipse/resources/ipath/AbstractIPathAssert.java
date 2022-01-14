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

package org.bndtools.test.assertj.eclipse.resources.ipath;

import static org.assertj.core.api.InstanceOfAssertFactories.FILE;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import java.util.Arrays;
import java.util.Objects;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.ListAssert;
import org.eclipse.core.runtime.IPath;

public abstract class AbstractIPathAssert<SELF extends AbstractIPathAssert<SELF, ACTUAL>, ACTUAL extends IPath>
	extends AbstractAssert<SELF, ACTUAL> {

	protected AbstractIPathAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF hasDevice(String expected) {
		isNotNull();
		if (!Objects.equals(actual.getDevice(), expected)) {
			throw failureWithActualExpected(actual.getDevice(), expected,
				"%nExpecting path%n <%s>%nto have device:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.getDevice());
		}
		return myself;
	}

	public AbstractStringAssert<?> hasDeviceThat() {
		return isNotNull().extracting(IPath::getDevice, STRING)
			.as(actual + ".device");
	}

	public SELF hasFileExtension(String expected) {
		isNotNull();
		if (!Objects.equals(actual.getFileExtension(), expected)) {
			throw failureWithActualExpected(actual.getFileExtension(), expected,
				"%nExpecting path%n <%s>%nto have file extension:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.getFileExtension());
		}
		return myself;
	}

	public AbstractStringAssert<?> hasFileExtensionThat() {
		return isNotNull().extracting(IPath::getFileExtension, STRING)
			.as(actual + ".fileExtension");
	}

	public SELF hasTrailingSeparator() {
		isNotNull();
		if (!actual.hasTrailingSeparator()) {
			throw failure("%nExpecting path%n  <%s>%nto have a trailing separator, but it does not", actual);
		}
		return myself;
	}

	public SELF doesNotHaveTrailingSeparator() {
		isNotNull();
		if (actual.hasTrailingSeparator()) {
			throw failure("%nExpecting path%n  <%s>%nto not have a trailing separator, but it does", actual);
		}
		return myself;
	}

	public SELF isAbsolute() {
		isNotNull();
		if (!actual.isAbsolute()) {
			throw failure("%nExpecting path%n  <%s>%nto be absolute, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotAbsolute() {
		isNotNull();
		if (actual.isAbsolute()) {
			throw failure("%nExpecting path%n  <%s>%nto not be absolute, but it is", actual);
		}
		return myself;
	}

	public SELF isEmpty() {
		isNotNull();
		if (!actual.isEmpty()) {
			throw failure("%nExpecting path%n  <%s>%nto be empty, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotEmpty() {
		isNotNull();
		if (actual.isEmpty()) {
			throw failure("%nExpecting path%n  <%s>%nto not be empty, but it is", actual);
		}
		return myself;
	}

	public SELF isPrefixOf(IPath anotherPath) {
		isNotNull();
		if (!actual.isPrefixOf(anotherPath)) {
			throw failure("%nExpecting path%n  <%s>%nto be a prefix of:%n  <%s>%n but it is not", actual);
		}
		return myself;
	}

	public SELF isNotPrefixOf(IPath anotherPath) {
		isNotNull();
		if (actual.isPrefixOf(anotherPath)) {
			throw failure("%nExpecting path%n  <%s>%nto not be a prefix of%n  <%s>%n but it is", actual);
		}
		return myself;
	}

	public SELF isRoot() {
		isNotNull();
		if (!actual.isRoot()) {
			throw failure("%nExpecting path%n  <%s>%nto be a root path, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotRoot() {
		isNotNull();
		if (actual.isRoot()) {
			throw failure("%nExpecting path%n  <%s>%nto not be a root path, but it is", actual);
		}
		return myself;
	}

	public SELF isUNC() {
		isNotNull();
		if (!actual.isUNC()) {
			throw failure("%nExpecting path%n  <%s>%nto be a UNC path, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotUNC() {
		isNotNull();
		if (actual.isUNC()) {
			throw failure("%nExpecting path%n  <%s>%nto not be a UNC path, but it is", actual);
		}
		return myself;
	}

	public SELF isValidPath(String path) {
		isNotNull();
		if (!actual.isValidPath(path)) {
			throw failure("%nExpecting path%n  <%s>%nto be valid, but it is not", path);
		}
		return myself;
	}

	public SELF isNotValidPath(String path) {
		isNotNull();
		if (actual.isValidPath(path)) {
			throw failure("%nExpecting path%n  <%s>%nto not be valid, but it is", path);
		}
		return myself;
	}

	public SELF isValidSegment(String segment) {
		isNotNull();
		if (!actual.isValidSegment(segment)) {
			throw failure("%nExpecting segment%n  <%s>%nto be valid, but it is not", segment);
		}
		return myself;
	}

	public SELF isNotValidSegment(String segment) {
		isNotNull();
		if (actual.isValidSegment(segment)) {
			throw failure("%nExpecting segment%n  <%s>%nto not be valid, but it is", segment);
		}
		return myself;
	}

	public SELF hasLastSegment(String expected) {
		isNotNull();
		if (!Objects.equals(actual.lastSegment(), expected)) {
			throw failureWithActualExpected(actual.lastSegment(), expected,
				"%nExpecting path%n <%s>%nto have last segment:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.lastSegment());
		}
		return myself;
	}

	public AbstractStringAssert<?> hasLastSegmentThat() {
		return isNotNull().extracting(IPath::lastSegment, STRING)
			.as(actual + ".lastSegment");
	}

	public SELF hasSegmentCount(int expected) {
		isNotNull();
		if (expected != actual.segmentCount()) {
			throw failureWithActualExpected(actual.segmentCount(), expected,
				"%nExpecting path%n <%s>%nto have segment count:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.segmentCount());
		}
		return myself;
	}

	public ListAssert<String> hasSegmentsThat() {
		return isNotNull().extracting(actual -> Arrays.asList(actual.segments()), list(String.class))
			.as(actual + ".segments");
	}

	public AbstractFileAssert<?> asFile() {
		return isNotNull().extracting(IPath::toFile, FILE)
			.as(actual + ".asFile");
	}

	public AbstractStringAssert<?> asOSString() {
		return isNotNull().extracting(IPath::toOSString, STRING)
			.as(actual + ".asOSString");
	}

	public AbstractStringAssert<?> asPortableString() {
		return isNotNull().extracting(IPath::toPortableString, STRING)
			.as(actual + ".asPortableString");
	}
}
