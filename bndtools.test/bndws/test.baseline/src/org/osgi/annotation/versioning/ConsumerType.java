/*
 * Copyright (c) OSGi Alliance (2013, 2014). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.annotation.versioning;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A type implemented by the Consumer Role.
 * <p>
 * A non-binary compatible change to a consumer type normally requires
 * incrementing the major version of the type's package. This change will
 * require all providers and all consumers to be updated to handle the change
 * since consumers implement the consumer type and all providers must understand
 * the change in the consumer type.
 * <p>
 * A type can be marked {@link ConsumerType} or {@link ProviderType} but not
 * both. A type is assumed to be {@link ConsumerType} if it is not marked either
 * {@link ConsumerType} or {@link ProviderType}.
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * understand the semantic version of a package. When a bundle implements a
 * consumer type from an imported package, then the bundle's import range for
 * that package must require the exact major version and a minor version greater
 * than or equal to the package's version.
 * 
 * @see <a href="http://www.osgi.org/wiki/uploads/Links/SemanticVersioning.pdf"
 *      > Semantic Versioning</a>
 * @author $Id$
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ConsumerType {
	// marker annotation
}
