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

import org.assertj.core.api.InstanceOfAssertFactory;
import org.eclipse.core.resources.IFile;

public class IFileAssert extends AbstractIFileAssert<IFileAssert, IFile> {

	public static final InstanceOfAssertFactory<IFile, IFileAssert> IFILE = new InstanceOfAssertFactory<>(
		IFile.class, IFileAssert::assertThat);

	public IFileAssert(IFile actual) {
		super(actual, IFileAssert.class);
	}

	public static IFileAssert assertThat(IFile actual) {
		return new IFileAssert(actual);
	}
}
