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

package org.bndtools.test.assertj.bndtools.imarker;

import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_CONTEXT_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_FILE_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_HEADER_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_PROJECT_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_REFERENCE_ATTR;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_PATH_PROBLEM;

import java.util.Objects;

import org.bndtools.test.assertj.eclipse.resources.imarker.AbstractIMarkerAssert;
import org.eclipse.core.resources.IMarker;

public abstract class AbstractBndPathProblemAssert<SELF extends AbstractBndPathProblemAssert<SELF, ACTUAL>, ACTUAL extends IMarker>
	extends AbstractIMarkerAssert<SELF, ACTUAL> {

	protected AbstractBndPathProblemAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	@Override
	public SELF isCorrectType() {
		isNotNull();
		return isSubtypeOf(MARKER_BND_PATH_PROBLEM);
	}

	public SELF hasBndtoolsContext(String expected) {
		isNotNull();
		String context = getStringAttribute(BNDTOOLS_MARKER_CONTEXT_ATTR);
		if (!Objects.equals(context, expected)) {
			throw failureWithActualExpected(context, expected,
				"%nExpecting%n  <%s>%nto have bndtools context:%n  <%s>%n but was:%n  <%s>", actual, expected, context);
		}
		return myself;
	}

	public SELF hasBndtoolsHeader(String expected) {
		isNotNull();
		String header = getStringAttribute(BNDTOOLS_MARKER_HEADER_ATTR);
		if (!Objects.equals(header, expected)) {
			throw failureWithActualExpected(header, expected,
				"%nExpecting%n  <%s>%nto have bndtools header:%n  <%s>%n but was:%n  <%s>", actual, expected, header);
		}
		return myself;
	}

	public SELF hasBndtoolsReference(String expected) {
		isNotNull();
		String reference = getStringAttribute(BNDTOOLS_MARKER_REFERENCE_ATTR);
		if (!Objects.equals(reference, expected)) {
			throw failureWithActualExpected(reference, expected,
				"%nExpecting%n  <%s>%nto have bndtools reference:%n  <%s>%n but was:%n  <%s>", actual, expected,
				reference);
		}
		return myself;
	}

	public SELF hasBndtoolsFile(String expected) {
		isNotNull();
		String file = getStringAttribute(BNDTOOLS_MARKER_FILE_ATTR);
		if (!Objects.equals(file, expected)) {
			throw failureWithActualExpected(file, expected,
				"%nExpecting%n  <%s>%nto have bndtools file:%n  <%s>%n but was:%n  <%s>", actual, expected, file);
		}
		return myself;
	}

	public SELF hasBndtoolsProject(String expected) {
		isNotNull();
		String project = getStringAttribute(BNDTOOLS_MARKER_PROJECT_ATTR);
		if (!Objects.equals(project, expected)) {
			throw failureWithActualExpected(project, expected,
				"%nExpecting%n  <%s>%nto have bndtools project:%n  <%s>%n but was:%n  <%s>", actual, expected, project);
		}
		return myself;
	}
}
