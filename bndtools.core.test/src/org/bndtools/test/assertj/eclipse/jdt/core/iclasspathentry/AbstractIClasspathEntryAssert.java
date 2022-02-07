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

import static org.bndtools.test.assertj.eclipse.resources.ipath.IPathAssert.IPATH;
import static org.eclipse.jdt.core.IClasspathEntry.CPE_CONTAINER;
import static org.eclipse.jdt.core.IClasspathEntry.CPE_LIBRARY;
import static org.eclipse.jdt.core.IClasspathEntry.CPE_PROJECT;
import static org.eclipse.jdt.core.IClasspathEntry.CPE_SOURCE;
import static org.eclipse.jdt.core.IClasspathEntry.CPE_VARIABLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ObjectArrayAssert;
import org.bndtools.test.assertj.eclipse.resources.ipath.AbstractIPathAssert;
import org.bndtools.test.assertj.eclipse.resources.iresource.IResourceAssert;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;

public abstract class AbstractIClasspathEntryAssert<SELF extends AbstractIClasspathEntryAssert<SELF, ACTUAL>, ACTUAL extends IClasspathEntry>
	extends AbstractAssert<SELF, ACTUAL> {

	protected AbstractIClasspathEntryAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public ObjectArrayAssert<IAccessRule> hasAccessRulesThat() {
		return isNotNull()
			.extracting(IClasspathEntry::getAccessRules, InstanceOfAssertFactories.array(IAccessRule[].class))
			.as(actual + ".accessRules");
	}

	public static String entryKind(int entryKind) {
		switch (entryKind) {
			case CPE_CONTAINER :
				return "CPE_CONTAINER (" + CPE_CONTAINER + ")";
			case CPE_LIBRARY :
				return "CPE_LIBRARY (" + CPE_LIBRARY + ")";
			case CPE_PROJECT :
				return "CPE_PROJECT (" + CPE_PROJECT + ")";
			case CPE_VARIABLE :
				return "CPE_VARIABLE (" + CPE_VARIABLE + ")";
			case CPE_SOURCE :
				return "CPE_VARIABLE (" + CPE_VARIABLE + ")";
			default :
				throw new IllegalArgumentException("Unrecognised classpath entry kind: " + entryKind);
		}
	}

	public SELF hasEntryKind(int expected) {
		isNotNull();
		// Do this here to trigger IAE if necessary
		final String expectedString = entryKind(expected);

		if (actual.getEntryKind() != expected) {
			final String actualString = entryKind(actual.getEntryKind());

			throw failureWithActualExpected(actualString, expectedString,
				"%nExpecting classpath%n  <%s>%nto have type:%n  <%s>%n but was:%n  <%s>", actual, expectedString,
				actualString);
		}
		return myself;
	}

	public <T extends AbstractIClasspathEntryAssert<T, S>, S extends IClasspathEntry> T hasEntryKind(
		InstanceOfAssertFactory<S, T> expected) {
		isNotNull();
		return expected.createAssert(actual)
			.isCorrectType();
	}

	public ICPEProjectAssert isProject() {
		return (this instanceof ICPEProjectAssert) ? (ICPEProjectAssert)myself : hasEntryKind(ICPEProjectAssert.CPE_PROJECT);
	}

	public ICPEProjectAssert refersToProject(String project) {
		return isProject().refersToProject(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject(project));
	}

	public ICPEProjectAssert refersToProject(IProject project) {
		ICPEProjectAssert castSelf = isProject();
		IPath actualPath = actual.getPath();
		IResource r = ResourcesPlugin.getWorkspace()
			.getRoot()
			.findMember(actual.getPath());

		if (r == null) {
			throw failure(
				"%nExpecting classpath%n  <%s>%nto refer to project:%n <%s>%nbut path:%n <%s>%ndoes not exist", actual,
				project, actualPath);
		}
		if (!(r instanceof IProject)) {
			throw failure("%nExpecting classpath%n  <%s>%nto refer to project:%n <%s>%nbut path:%n <%s>%nwas a %s",
				actual, project, actualPath, IResourceAssert.typeToString(r.getType()));
		}
		if (!Objects.equals(r, project)) {
			throw failureWithActualExpected(r, project,
				"%nExpecting classpath%n  <%s>%nto refer to project:%n <%s>%nbut it referred to:%n <%s>", actual,
				project, r);
		}
		return castSelf;
	}

	public SELF isLibrary() {
		return hasEntryKind(IClasspathEntry.CPE_LIBRARY);
	}

	// This should not be exposed publicly; the public interface is via
	// hasEntryKind(InstanceOfAssertFactory)
	protected SELF isCorrectType() {
		return myself;
	}

	public SELF hasClasspathAttribute(IClasspathAttribute attribute) {
		return hasClasspathAttribute(attribute.getName(), attribute.getValue());
	}

	public SELF hasClasspathAttribute(String name, String value) {
		isNotNull();
		final IClasspathAttribute[] attributes = actual.getExtraAttributes();
		final List<String> found = new ArrayList<>(attributes.length);
		for (IClasspathAttribute attribute : attributes) {
			if (Objects.equals(name, attribute.getName())) {
				if (!Objects.equals(value, attribute.getValue())) {
					found.add(attribute.getValue());
				} else {
					return myself;
				}
			}
		}
		if (found.isEmpty()) {
			throw failure(
				"%nExpecting classpath%n  <%s>%nto have attribute:%n  <[%s=%s]>%n but there was no attribute with that name",
				actual, name, value);
		} else {
			throw failure(
				"%nExpecting classpath%n  <%s>%nto have attribute:%n  <[%s=%s]>%n but the following values were found with that name:%n  <%s>",
				actual, name, value, found);
		}
	}

	public SELF hasClasspathAttribute(String name) {
		isNotNull();
		for (IClasspathAttribute attribute : actual.getExtraAttributes()) {
			if (Objects.equals(name, attribute.getName())) {
				return myself;
			}
		}
		throw failure("%nExpecting classpath%n  <%s>%nto have attribute:%n  <[%s]>%n but it does not", actual, name);
	}

	public SELF doesNotHaveClasspathAttribute(String name) {
		isNotNull();
		final IClasspathAttribute[] attributes = actual.getExtraAttributes();
		final List<String> found = new ArrayList<>(attributes.length);
		for (IClasspathAttribute attribute : attributes) {
			if (Objects.equals(name, attribute.getName())) {
				found.add(attribute.getValue());
			}
		}
		if (!found.isEmpty()) {
			throw failure(
				"%nExpecting classpath%n  <%s>%nto have not attribute:%n  <[%s]>%n but the following values were found with that name:%n  <%s>",
				actual, name, found);
		}
		return myself;
	}

	// IPath[] getExclusionPatterns();
	// IPath[] getInclusionPatterns();
	public SELF hasOutputLocation(String expected) {
		return hasOutputLocation(new Path(expected));
	}

	public SELF hasOutputLocation(IPath expected) {
		isNotNull();
		if (!Objects.equals(actual.getOutputLocation(), expected)) {
			throw failureWithActualExpected(actual.getOutputLocation(), expected,
				"%nExpecting%n  <%s>%nto have output location:%n  <%s>%n but was:%n  <%s>", actual, expected,
				actual.getOutputLocation());
		}
		return myself;
	}

	public SELF hasPath(String expected) {
		return hasPath(new Path(expected));
	}

	public SELF hasPath(IPath expected) {
		isNotNull();
		if (!Objects.equals(actual.getPath(), expected)) {
			throw failureWithActualExpected(actual.getPath(), expected,
				"%nExpecting%n  <%s>%nto have path:%n  <%s>%n but was:%n  <%s>", actual, expected, actual.getPath());
		}
		return myself;
	}

	public AbstractIPathAssert<?, ?> hasPathThat() {
		return isNotNull().extracting(t -> t.getPath(), IPATH)
			.as(actual + ".path");
	}

	public SELF hasSourceAttachmentPath(String expected) {
		return hasSourceAttachmentPath(new Path(expected));
	}

	public SELF hasSourceAttachmentPath(IPath expected) {
		isNotNull();
		if (!Objects.equals(actual.getSourceAttachmentPath(), expected)) {
			throw failureWithActualExpected(actual.getSourceAttachmentPath(), expected,
				"%nExpecting%n  <%s>%nto have source attachement path:%n  <%s>%n but was:%n  <%s>", actual, expected,
				actual.getSourceAttachmentPath());
		}
		return myself;
	}

	public SELF hasSourceAttachmentRootPath(String expected) {
		return hasSourceAttachmentRootPath(new Path(expected));
	}

	public SELF hasSourceAttachmentRootPath(IPath expected) {
		isNotNull();
		if (!Objects.equals(actual.getSourceAttachmentRootPath(), expected)) {
			throw failureWithActualExpected(actual.getSourceAttachmentRootPath(), expected,
				"%nExpecting%n  <%s>%nto have source attachement root path:%n  <%s>%n but was:%n  <%s>", actual,
				expected, actual.getSourceAttachmentRootPath());
		}
		return myself;
	}

	public SELF isExported() {
		isNotNull();
		if (!actual.isExported()) {
			throw failure("%nExpecting classpath%n  <%s>%nto be exported, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotExported() {
		isNotNull();
		if (actual.isExported()) {
			throw failure("%nExpecting classpath%n  <%s>%nto not be exported, but it is", actual);
		}
		return myself;
	}

	public SELF isTest() {
		isNotNull();
		if (!actual.isTest()) {
			throw failure("%nExpecting classpath%n  <%s>%nto be on the test classpath only, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotTest() {
		isNotNull();
		if (actual.isTest()) {
			throw failure("%nExpecting classpath%n  <%s>%nto not be on the test classpath only, but it is", actual);
		}
		return myself;
	}

	public SELF isWithoutTestCode() {
		isNotNull();
		if (!actual.isWithoutTestCode()) {
			throw failure(
				"%nExpecting classpath%n  <%s>%nto not include the test code of the referenced project, but it does",
				actual);
		}
		return myself;
	}

	public SELF isNotWithoutTestCode() {
		isNotNull();
		if (actual.isWithoutTestCode()) {
			throw failure(
				"%nExpecting classpath%n  <%s>%nto include the test code of the referenced project, but it does not",
				actual);
		}
		return myself;
	}
}
