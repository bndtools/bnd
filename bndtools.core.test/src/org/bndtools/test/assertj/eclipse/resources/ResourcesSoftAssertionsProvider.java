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

package org.bndtools.test.assertj.eclipse.resources;

import org.bndtools.test.assertj.eclipse.resources.icontainer.IContainerSoftAssertionsProvider;
import org.bndtools.test.assertj.eclipse.resources.ifile.IFileSoftAssertionsProvider;
import org.bndtools.test.assertj.eclipse.resources.imarker.IMarkerSoftAssertionsProvider;
import org.bndtools.test.assertj.eclipse.resources.ipath.IPathSoftAssertionsProvider;
import org.bndtools.test.assertj.eclipse.resources.iproject.IProjectSoftAssertionsProvider;
import org.bndtools.test.assertj.eclipse.resources.iresource.IResourceSoftAssertionsProvider;
import org.bndtools.test.assertj.eclipse.resources.ischedulingrule.ISchedulingRuleSoftAssertionsProvider;

public interface ResourcesSoftAssertionsProvider extends IContainerSoftAssertionsProvider,
	IResourceSoftAssertionsProvider, IFileSoftAssertionsProvider, IMarkerSoftAssertionsProvider,
	IPathSoftAssertionsProvider, IProjectSoftAssertionsProvider, ISchedulingRuleSoftAssertionsProvider {}
