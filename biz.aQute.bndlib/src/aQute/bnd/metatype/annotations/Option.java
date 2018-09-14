/*
 * Copyright (c) OSGi Alliance (2013, 2016). All Rights Reserved.
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

package aQute.bnd.metatype.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Option} information for an {@link AttributeDefinition}.
 * 
 * @see AttributeDefinition#options()
 * @author $Id$
 */
@Retention(RetentionPolicy.CLASS)
@Target({})
public @interface Option {

	/**
	 * The human readable label of this Option.
	 * 
	 * <p>
	 * If not specified, the label of this Option is the empty string.
	 * 
	 * <p>
	 * If the label begins with the percent sign ({@code '%'} &#92;u0025), the
	 * label can be {@link ObjectClassDefinition#localization() localized}.
	 * 
	 * @see "The label attribute of the Option element of a Meta Type Resource."
	 */
	String label() default "";

	/**
	 * The value of this Option.
	 * 
	 * @see "The value attribute of the Option element of a Meta Type Resource."
	 */
	String value();
}
