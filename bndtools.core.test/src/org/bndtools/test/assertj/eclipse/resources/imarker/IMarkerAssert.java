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

import org.assertj.core.api.InstanceOfAssertFactory;
import org.eclipse.core.resources.IMarker;

public class IMarkerAssert extends AbstractIMarkerAssert<IMarkerAssert, IMarker> {

	public static final InstanceOfAssertFactory<IMarker, IMarkerAssert> IMARKER = new InstanceOfAssertFactory<>(
		IMarker.class, IMarkerAssert::assertThat);

	public IMarkerAssert(IMarker actual) {
		super(actual, IMarkerAssert.class);
	}

	public static IMarkerAssert assertThat(IMarker actual) {
		return new IMarkerAssert(actual);
	}
}
