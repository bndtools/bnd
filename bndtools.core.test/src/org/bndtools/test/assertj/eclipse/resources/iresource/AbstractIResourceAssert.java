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

package org.bndtools.test.assertj.eclipse.resources.iresource;

import static org.assertj.core.api.InstanceOfAssertFactories.LONG;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.URI_TYPE;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.bndtools.test.assertj.eclipse.resources.ipath.IPathAssert.IPATH;
import static org.eclipse.core.resources.IResource.CHECK_ANCESTORS;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import org.assertj.core.api.AbstractDateAssert;
import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.AbstractUriAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
import org.bndtools.test.assertj.eclipse.resources.ipath.IPathAssert;
import org.bndtools.test.assertj.eclipse.resources.ischedulingrule.AbstractISchedulingRuleAssert;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.osgi.test.common.exceptions.Exceptions;

public abstract class AbstractIResourceAssert<SELF extends AbstractIResourceAssert<SELF, ACTUAL>, ACTUAL extends IResource>
	extends AbstractISchedulingRuleAssert<SELF, ACTUAL> {

	protected AbstractIResourceAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	// TODO: When osgi-test 1.1.0 is available, use the method from there.
	public static long getTime(Object expected) {
		if (expected == null) {
			throw new IllegalArgumentException("expected cannot be null");
		} else if (expected instanceof Long) {
			return (Long) expected;
		} else if (expected instanceof Date) {
			return ((Date) expected).getTime();
		} else if (expected instanceof Instant) {
			return ((Instant) expected).toEpochMilli();
		} else {
			throw new IllegalArgumentException("Expected must be a long, Date or Instant");
		}
	}

	public static final InstanceOfAssertFactory<Long, AbstractDateAssert<?>> LONG_AS_DATE = new InstanceOfAssertFactory<>(
		Long.class, date -> Assertions.assertThat(new Date(date)));

	public SELF exists() {
		isNotNull();
		if (!actual.exists()) {
			throw failure("%nExpecting resource:%n  <%s>%nto exist, but it does not", actual);
		}
		return myself;
	}

	public SELF doesNotExist() {
		isNotNull();
		if (actual.exists()) {
			throw failure("%nExpecting resource:%n  <%s>%nto not exist, but it does", actual);
		}
		return myself;
	}

	public SELF hasFullPath(IPath expected) {
		isNotNull();
		if (!Objects.equals(actual.getFullPath(), expected)) {
			throw failure("%nExpecting%n <%s>%nto have full path:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.getFullPath());
		}
		return myself;
	}

	public IPathAssert hasFullPathThat() {
		return isNotNull().extracting(IResource::getFullPath, IPATH)
			.as(actual + ".fullPath");
	}

	public SELF hasLocalTimeStamp(Object expected) {
		isNotNull();
		long expectedTime = getTime(expected);
		if (actual.getLocalTimeStamp() != expectedTime) {
			throw failure("%nExpecting%n <%s>%nto have local timestamp:%n  <%d>%n but it was:%n  <%d>", actual,
				expectedTime, actual.getLocalTimeStamp());
		}
		return myself;
	}

	public AbstractLongAssert<?> hasLocalTimeStampLongThat() {
		return isNotNull().extracting(IResource::getLocalTimeStamp, LONG)
			.as(actual + ".localTimeStamp");
	}

	public SELF hasLocation(IPath expected) {
		isNotNull();
		if (!Objects.equals(actual.getLocation(), expected)) {
			throw failure("%nExpecting%n <%s>%nto have location:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.getLocation());
		}
		return myself;
	}

	public IPathAssert hasLocationThat() {
		return isNotNull().extracting(IResource::getLocation, IPATH)
			.as(actual + ".location");
	}

	public SELF hasLocationURI(URI expected) {
		isNotNull();
		if (!Objects.equals(actual.getLocationURI(), expected)) {
			throw failure("%nExpecting%n <%s>%nto have location URI:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.getLocationURI());
		}
		return myself;
	}

	public AbstractUriAssert<?> hasLocationURIThat() {
		return isNotNull().extracting(IResource::getLocationURI, URI_TYPE)
			.as(actual + ".locationURI");
	}

	public SELF hasName(String expected) {
		isNotNull();
		if (!Objects.equals(actual.getName(), expected)) {
			throw failureWithActualExpected(actual.getName(), expected,
				"%nExpecting%n <%s>%nto have name:%n  <%s>%n but it was:%n  <%s>", actual, expected, actual.getName());
		}
		return myself;
	}

	public AbstractStringAssert<?> hasNameThat() {
		return isNotNull().extracting(IResource::getName, STRING)
			.as(actual + ".name");
	}

	public SELF hasModificationStamp(long expected) {
		isNotNull();
		if (actual.getModificationStamp() != expected) {
			throw failureWithActualExpected(actual.getModificationStamp(), expected,
				"%nExpecting%n <%s>%nto have modification stamp:%n  <%d>%n but it was:%n  <%d>", actual, expected,
				actual.getModificationStamp());
		}
		return myself;
	}

	public SELF doesNotHaveModificationStamp(long expected) {
		isNotNull();
		if (actual.getModificationStamp() == expected) {
			throw failure("%nExpecting%n <%s>%nto not have modification stamp:%n  <%d>%n but it does", actual,
				expected);
		}
		return myself;
	}

	public SELF hasParent(IContainer expected) {
		isNotNull();
		if (!Objects.equals(actual.getParent(), expected)) {
			throw failureWithActualExpected(actual.getParent(), expected,
				"%nExpecting resource%n  <%s>%nto have parent:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.getParent());
		}
		return myself;
	}

	// TODO: hasParentThat()

	public MapAssert<QualifiedName, String> hasPersistentPropertiesThat() {
		return isNotNull().extracting(t -> {
			try {
				return t.getPersistentProperties();
			} catch (CoreException e) {
				throw Exceptions.duck(e);
			}
		}, map(QualifiedName.class, String.class))
			.as(actual + ".persistentProperties");
	}

	public SELF hasProjectRelativePath(IPath expected) {
		isNotNull();
		if (!Objects.equals(actual.getProjectRelativePath(), expected)) {
			throw failure("%nExpecting%n <%s>%nto have project-relative path:%n  <%s>%n but it was:%n  <%s>", actual,
				expected, actual.getProjectRelativePath());
		}
		return myself;
	}

	public IPathAssert hasProjectRelativePathThat() {
		return isNotNull().extracting(IResource::getProjectRelativePath, IPATH)
			.as(actual + ".projectRelativePath");
	}

	public SELF hasRawLocation(IPath expected) {
		isNotNull();
		if (!Objects.equals(actual.getRawLocation(), expected)) {
			throw failure("%nExpecting%n <%s>%nto have raw location:%n  <%s>%n but it was:%n  <%s>", actual, expected,
				actual.getRawLocation());
		}
		return myself;
	}

	public IPathAssert hasRawLocationThat() {
		return isNotNull().extracting(IResource::getRawLocation, IPATH)
			.as(actual + ".rawLocation");
	}

	public SELF hasRawLocationURI(URI expected) {
		isNotNull();
		if (!Objects.equals(actual.getRawLocationURI(), expected)) {
			throw failure("%nExpecting%n <%s>%nto have raw location URI:%n  <%s>%n but it was:%n  <%s>", actual,
				expected, actual.getRawLocationURI());
		}
		return myself;
	}

	public AbstractUriAssert<?> hasRawLocationURIThat() {
		return isNotNull().extracting(IResource::getRawLocationURI, URI_TYPE)
			.as(actual + ".rawLocationURI");
	}

	public MapAssert<QualifiedName, String> hasSessionPropertiesThat() {
		return isNotNull().extracting(t -> {
			try {
				return t.getSessionProperties();
			} catch (CoreException e) {
				throw Exceptions.duck(e);
			}
		}, map(QualifiedName.class, String.class))
			.as(actual + ".sessionProperties");
	}

	public SELF isAccessible() {
		isNotNull();
		if (!actual.isAccessible()) {
			throw failure("%nExpecting resource%n  <%s>%nto be accessible, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotAccessible() {
		isNotNull();
		if (actual.isAccessible()) {
			throw failure("%nExpecting resource%n  <%s>%nto not be accessible, but it was", actual);
		}
		return myself;
	}

	public SELF isDerived() {
		isNotNull();
		if (!actual.isDerived()) {
			throw failure("%nExpecting resource%n  <%s>%nto be derived, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotDerived() {
		isNotNull();
		if (actual.isDerived()) {
			throw failure("%nExpecting resource%n  <%s>%nto not be derived, but it was", actual);
		}
		return myself;
	}

	public SELF isDerivedWithInheritance() {
		isNotNull();
		if (!actual.isDerived(CHECK_ANCESTORS)) {
			throw failure(
				"%nExpecting resource%n  <%s>%nto be derived or to be the descendent of a derived resource, but it is not",
				actual);
		}
		return myself;
	}

	public SELF isNotDerivedWithInheritance() {
		isNotNull();
		if (actual.isDerived(CHECK_ANCESTORS)) {
			throw failure(
				"%nExpecting resource%n  <%s>%nto not be derived or the descendent of a derived resource, but it was",
				actual);
		}
		return myself;
	}

	public SELF isHidden() {
		isNotNull();
		if (!actual.isHidden()) {
			throw failure("%nExpecting resource%n  <%s>%nto be hidden, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotHidden() {
		isNotNull();
		if (actual.isHidden()) {
			throw failure("%nExpecting resource%n  <%s>%nto not be hidden, but it was", actual);
		}
		return myself;
	}

	public SELF isHiddenWithInheritance() {
		isNotNull();
		if (!actual.isHidden(CHECK_ANCESTORS)) {
			throw failure(
				"%nExpecting resource%n  <%s>%nto be hidden or to be the descendent of a hidden resource, but it is not",
				actual);
		}
		return myself;
	}

	public SELF isNotHiddenWithInheritance() {
		isNotNull();
		if (actual.isHidden(CHECK_ANCESTORS)) {
			throw failure(
				"%nExpecting resource%n  <%s>%nto not be hidden or the descendent of a hidden resource, but it was",
				actual);
		}
		return myself;
	}

	public SELF isLinked() {
		isNotNull();
		if (!actual.isLinked()) {
			throw failure("%nExpecting resource%n  <%s>%nto be linked, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotLinked() {
		isNotNull();
		if (actual.isLinked()) {
			throw failure("%nExpecting resource%n  <%s>%nto not be linked, but it was", actual);
		}
		return myself;
	}

	public SELF isLinkedWithInheritance() {
		isNotNull();
		if (!actual.isLinked(CHECK_ANCESTORS)) {
			throw failure(
				"%nExpecting resource%n  <%s>%nto be linked or to be the descendent of a linked resource, but it is not",
				actual);
		}
		return myself;
	}

	public SELF isNotLinkedWithInheritance() {
		isNotNull();
		if (actual.isLinked(CHECK_ANCESTORS)) {
			throw failure(
				"%nExpecting resource%n  <%s>%nto not be linked or the descendent of a linked resource, but it was",
				actual);
		}
		return myself;
	}

	public SELF isSynchronizedDepth0() {
		isNotNull();
		if (!actual.isSynchronized(IResource.DEPTH_ZERO)) {
			throw failure("%nExpecting resource%n  <%s>%nto be synchronized to depth 0, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotSyncronizedDepth0() {
		isNotNull();
		if (actual.isSynchronized(IResource.DEPTH_ZERO)) {
			throw failure("%nExpecting resource%n  <%s>%nto not be synchronized to depth 0, but it was", actual);
		}
		return myself;
	}

	public SELF isSynchronizedDepth1() {
		isNotNull();
		if (!actual.isSynchronized(IResource.DEPTH_ONE)) {
			throw failure("%nExpecting resource%n  <%s>%nto be synchronized to depth 1, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotSyncronizedDepth1() {
		isNotNull();
		if (actual.isSynchronized(IResource.DEPTH_ONE)) {
			throw failure("%nExpecting resource%n  <%s>%nto not be synchronized to depth 1, but it was", actual);
		}
		return myself;
	}

	public SELF isSynchronizedFullDepth() {
		isNotNull();
		if (!actual.isSynchronized(IResource.DEPTH_INFINITE)) {
			throw failure("%nExpecting resource%n  <%s>%nto be synchronized to infinite depth, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotSyncronizedFullDepth() {
		isNotNull();
		if (actual.isSynchronized(IResource.DEPTH_INFINITE)) {
			throw failure("%nExpecting resource%n  <%s>%nto not be synchronized to infinite depth, but it was", actual);
		}
		return myself;
	}

	public SELF isTeamPrivateMember() {
		isNotNull();
		if (!actual.isTeamPrivateMember()) {
			throw failure("%nExpecting resource%n  <%s>%nto be team private member, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotTeamPrivateMember() {
		isNotNull();
		if (actual.isTeamPrivateMember()) {
			throw failure("%nExpecting resource%n  <%s>%nto not be team private member, but it was", actual);
		}
		return myself;
	}

	public SELF isTeamPrivateMemberWithInheritance() {
		isNotNull();
		if (!actual.isTeamPrivateMember(CHECK_ANCESTORS)) {
			throw failure(
				"%nExpecting resource%n  <%s>%nto be team private member or to be the descendent of a team private member resource, but it is not",
				actual);
		}
		return myself;
	}

	public SELF isNotTeamPrivateMemberWithInheritance() {
		isNotNull();
		if (actual.isTeamPrivateMember(CHECK_ANCESTORS)) {
			throw failure(
				"%nExpecting resource%n  <%s>%nto not be team private member or the descendent of a team private member resource, but it was",
				actual);
		}
		return myself;
	}

	public SELF isPhantom() {
		isNotNull();
		if (!actual.isPhantom()) {
			throw failure("%nExpecting resource%n  <%s>%nto be phantom, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotPhantom() {
		isNotNull();
		if (actual.isPhantom()) {
			throw failure("%nExpecting resource%n  <%s>%nto not be phantom, but it was", actual);
		}
		return myself;
	}

	public SELF isVirtual() {
		isNotNull();
		if (!actual.isVirtual()) {
			throw failure("%nExpecting resource%n  <%s>%nto be virtual, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotVirtual() {
		isNotNull();
		if (actual.isVirtual()) {
			throw failure("%nExpecting resource%n  <%s>%nto not be virtual, but it was", actual);
		}
		return myself;
	}
}
