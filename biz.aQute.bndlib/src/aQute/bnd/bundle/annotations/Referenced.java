/*
 * Copyright (c) OSGi Alliance (2020). All Rights Reserved.
 *
 * Licensed under the Apache License, Export 2.0 (the "License");
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark types as referenced.
 * <p>
 * A reference can cause the package of a specified type to be imported if the
 * bundle does not contain the package.
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
public @interface Referenced {
	/**
	 * A list of referenced classes.
	 * <p>
	 * Specifying a class in this annotation must be treated by tools as if the
	 * annotated type has a code reference to the class which may result in an
	 * import of the package of the class if the bundle does not contain that
	 * package.
	 */
	Class<?>[] value();
}
