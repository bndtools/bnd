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

package org.bndtools.test.assertj.eclipse.resources.ifile;

import static org.assertj.core.api.InstanceOfAssertFactories.INPUT_STREAM;

import java.util.Objects;

import org.assertj.core.api.AbstractInputStreamAssert;
import org.bndtools.test.assertj.eclipse.resources.iresource.AbstractIResourceAssert;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import aQute.bnd.exceptions.Exceptions;

public abstract class AbstractIFileAssert<SELF extends AbstractIFileAssert<SELF, ACTUAL>, ACTUAL extends IFile>
	extends AbstractIResourceAssert<SELF, ACTUAL> {

	protected AbstractIFileAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isReadOnly() {
		isNotNull();
		if (!actual.isReadOnly()) {
			throw failure("%nExpecting file:%n  <%s>%nto be read only, but it is not", actual);
		}
		return myself;
	}

	public SELF isNotReadOnly() {
		isNotNull();
		if (actual.isReadOnly()) {
			throw failure("%nExpecting file:%n  <%s>%nto not be read only, but it was", actual);
		}
		return myself;
	}

	public AbstractInputStreamAssert<?, ?> hasContentsThat() {
		return isNotNull().extracting(t -> {
			try {
				return t.getContents();
			} catch (CoreException e) {
				throw Exceptions.duck(e);
			}
		}, INPUT_STREAM)
			.as(actual + ".contents");
	}

	public SELF hasCharset(String expected) {
		isNotNull();
		try {
			if (!Objects.equals(actual.getCharset(), expected)) {
				throw failureWithActualExpected(actual.getCharset(), expected,
					"%nExpecting file:%n <%s>%nto have name:%n  <%s>%n but it was:%n  <%s>", actual, expected,
					actual.getName());
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		return myself;
	}
}
