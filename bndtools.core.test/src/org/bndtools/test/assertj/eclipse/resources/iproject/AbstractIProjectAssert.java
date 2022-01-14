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

package org.bndtools.test.assertj.eclipse.resources.iproject;

import static org.assertj.core.api.InstanceOfAssertFactories.list;

import java.util.Arrays;
import java.util.Objects;

import org.assertj.core.api.ListAssert;
import org.bndtools.test.assertj.eclipse.resources.iresource.AbstractIResourceAssert;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import aQute.bnd.exceptions.Exceptions;

public abstract class AbstractIProjectAssert<SELF extends AbstractIProjectAssert<SELF, ACTUAL>, ACTUAL extends IProject>
	extends AbstractIResourceAssert<SELF, ACTUAL> {

	protected AbstractIProjectAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF hasActiveBuildConfig(IBuildConfiguration expected) {
		isNotNull();
		try {
			if (!Objects.equals(actual.getActiveBuildConfig(), expected)) {
				throw failureWithActualExpected(actual.getActiveBuildConfig(), expected,
					"%nExpecting%n <%s>%nto have active build config:%n  <%s>%n but it was:%n  <%s>", actual, expected,
					actual.getActiveBuildConfig());
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public ListAssert<IProject> hasReferencedProjectsThat() {
		return isOpen().extracting(actual -> {
			try {
				return Arrays.asList(actual.getReferencedProjects());
			} catch (CoreException e) {
				throw Exceptions.duck(e);
			}
		}, list(IProject.class))
			.as(actual + ".referencedProjects");
	}

	public ListAssert<IProject> hasReferencingProjectsThat() {
		return isOpen().extracting(actual -> Arrays.asList(actual.getReferencingProjects()), list(IProject.class))
			.as(actual + ".referencingProjects");
	}

	public SELF hasBuildConfig(String configName) {
		isOpen();
		try {
			if (!actual.hasBuildConfig(configName)) {
				throw failure("%nExpecting project:%n  <%s>%nto have build config named:%n  <%s>%n but it does not",
					actual, configName);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF doesNotHaveBuildConfig(String configName) {
		isOpen();
		try {
			if (actual.hasBuildConfig(configName)) {
				throw failure("%nExpecting project:%n  <%s>%nto not have build config named:%n  <%s>%n but it does",
					actual, configName);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF hasNature(String natureId) {
		isOpen();
		try {
			if (!actual.hasNature(natureId)) {
				throw failure("%nExpecting project:%n  <%s>%nto have nature:%n  <%s>%n but it does not", actual,
					natureId);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF doesNotHaveNature(String natureId) {
		isOpen();
		try {
			if (actual.hasNature(natureId)) {
				throw failure("%nExpecting project:%n  <%s>%nto not have nature:%n  <%s>%n but it does", actual,
					natureId);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF isNatureEnabled(String natureId) {
		hasNature(natureId);
		try {
			if (!actual.isNatureEnabled(natureId)) {
				throw failure("%nExpecting project:%n  <%s>%nto have nature:%n  <%s>%n enabled, but it is not", actual,
					natureId);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF isNatureNotEnabled(String natureId) {
		isOpen();
		try {
			if (actual.isNatureEnabled(natureId)) {
				throw failure("%nExpecting project:%n  <%s>%nto not have nature:%n  <%s>%n enabled, but it does",
					actual, natureId);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}

	public SELF isOpen() {
		isNotNull();
		if (!actual.isOpen()) {
			throw failure("%nExpecting project:%n  <%s>%nto be open, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotOpen() {
		isNotNull();
		if (actual.isOpen()) {
			throw failure("%nExpecting project:%n  <%s>%nto notbe open, but it is", actual);
		}
		return myself;
	}
}
