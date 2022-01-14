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

import static org.bndtools.test.assertj.eclipse.resources.ifile.IFileAssert.IFILE;
import static org.bndtools.test.assertj.eclipse.resources.iproject.IProjectAssert.IPROJECT;

import org.assertj.core.api.InstanceOfAssertFactory;
import org.bndtools.test.assertj.eclipse.resources.ifile.IFileAssert;
import org.bndtools.test.assertj.eclipse.resources.iproject.IProjectAssert;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public class IResourceAssert extends AbstractIResourceAssert<IResourceAssert, IResource> {

	public static final InstanceOfAssertFactory<IResource, IResourceAssert> IRESOURCE = new InstanceOfAssertFactory<>(
		IResource.class, IResourceAssert::assertThat);

	public IResourceAssert(IResource actual) {
		super(actual, IResourceAssert.class);
	}

	public static IResourceAssert assertThat(IResource actual) {
		return new IResourceAssert(actual);
	}

	public IResourceAssert hasType(int expected) {
		isNotNull();
		if (actual.getType() != expected) {
			throw failureWithActualExpected(actual.getType(), expected,
				"%nExpecting%n  <%s>%nto have type:%n  <%d:%s>%n but was:%n  <%d:%s>", actual, typeToString(expected),
				expected, typeToString(actual.getType()), actual.getType());
		}
		return myself;
	}

	// Convenience for pre-defined types
	public IResourceAssert isFile() {
		return hasType(IResource.FILE);
	}

	public IResourceAssert isFolder() {
		return hasType(IResource.FOLDER);
	}

	public IResourceAssert isProject() {
		return hasType(IResource.PROJECT);
	}

	public IResourceAssert isRoot() {
		return hasType(IResource.ROOT);
	}

	private static String typeToString(int type) {
		switch (type) {
			case IResource.FILE :
				return "FILE";
			case IResource.FOLDER :
				return "FOLDER";
			case IResource.PROJECT :
				return "PROJECT";
			case IResource.ROOT :
				return "ROOT";
			default :
				return "UNKNOWN";
		}
	}

	public IFileAssert asFile() {
		return isFile().extracting(a -> (IFile) a, IFILE);
	}

	public IProjectAssert asProject() {
		return isProject().extracting(a -> (IProject) a, IPROJECT);
	}
}
