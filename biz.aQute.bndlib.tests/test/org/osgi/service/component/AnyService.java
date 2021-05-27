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

package org.osgi.service.component;

/**
 * A marker type whose name is used in the {@code interface} attribute of a
 * {@code reference} element in a component description to indicate that the
 * type of the service for a reference is not specified and can thus be any
 * service type.
 * <p>
 * When specifying this marker type in the {@code interface} attribute of a
 * {@code reference} element in a component description:
 * <ul>
 * <li>The service type of the reference member or parameter must be
 * {@code java.lang.Object} so that any service object can be provided.</li>
 * <li>The {@code target} attribute of the {@code reference} element must be
 * specified to constrain the target services.</li>
 * </ul>
 * <p>
 * For example:
 * 
 * <pre>
 * &#64;Reference(service = AnyService.class, target = "(osgi.jaxrs.extension=true)")
 * List&lt;Object&gt; extensions;
 * </pre>
 * 
 * @since 1.5
 */
public final class AnyService {
	private AnyService() {
		// do not allow object creation
	}
}
