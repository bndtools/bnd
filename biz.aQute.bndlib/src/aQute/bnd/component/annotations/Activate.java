/*
 * Copyright (c) OSGi Alliance (2011, 2017). All Rights Reserved.
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

package aQute.bnd.component.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identify the annotated member as part of the activation of a Service
 * Component.
 * <p>
 * When this annotation is applied to a:
 * <ul>
 * <li>Method - The method is the {@code activate} method of the Component.</li>
 * <li>Constructor - The constructor will be used to construct the Component and
 * can be called with activation objects and bound services as parameters.</li>
 * <li>Field - The field will contain an activation object of the Component. The
 * field must be set after the constructor is called and before calling any
 * other method on the fully constructed component instance. That is, there is a
 * <i>happens-before</i> relationship between the field being set and calling
 * any method on the fully constructed component instance such as the
 * {@code activate} method.</li>
 * </ul>
 * <p>
 * This annotation is not processed at runtime by Service Component Runtime. It
 * must be processed by tools and used to add a Component Description to the
 * bundle.
 * 
 * @see "The init, activate, and activation-fields attributes of the component element of a Component Description."
 * @author $Id$
 * @since 1.1
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR
})
public @interface Activate {
	// marker annotation
}
