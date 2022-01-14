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

package org.bndtools.test.assertj.eclipse.resources.ifolder;

import org.assertj.core.api.InstanceOfAssertFactory;
import org.eclipse.core.resources.IFolder;

public class IFolderAssert extends AbstractIFolderAssert<IFolderAssert, IFolder> {

	public static final InstanceOfAssertFactory<IFolder, IFolderAssert> IFILE = new InstanceOfAssertFactory<>(
		IFolder.class, IFolderAssert::assertThat);

	public IFolderAssert(IFolder actual) {
		super(actual, IFolderAssert.class);
	}

	public static IFolderAssert assertThat(IFolder actual) {
		return new IFolderAssert(actual);
	}
}
