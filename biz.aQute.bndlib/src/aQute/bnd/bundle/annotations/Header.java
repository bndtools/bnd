/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
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

package aQute.bnd.bundle.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a manifest header for a bundle.
 * <p>
 * For example:
 *
 * <pre>
 * &#64;Header(name=Constants.BUNDLE_CATEGORY, value="osgi")
 * </pre>
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests.
 *
 * @author $Id$
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.TYPE, ElementType.PACKAGE
})
@Repeatable(Headers.class)
public @interface Header {

	/**
	 * The name of this header.
	 */
	String name();

	/**
	 * The value of this header.
	 */
	String value();
}
