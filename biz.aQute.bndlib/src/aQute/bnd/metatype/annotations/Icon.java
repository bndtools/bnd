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
 * {@code Icon} information for an {@link ObjectClassDefinition}.
 * 
 * @see ObjectClassDefinition#icon()
 * @author $Id$
 */
@Retention(RetentionPolicy.CLASS)
@Target({})
public @interface Icon {

	/**
	 * The resource name for this Icon.
	 * 
	 * <p>
	 * The resource is a URL. The resource URL can be relative to the root of
	 * the bundle containing the Meta Type Resource.
	 * 
	 * <p>
	 * If the resource begins with the percent sign ({@code '%'} &#92;u0025),
	 * the resource can be {@link ObjectClassDefinition#localization()
	 * localized}.
	 * 
	 * @see "The resource attribute of the Icon element of a Meta Type Resource."
	 */
	String resource();

	/**
	 * The pixel size of this Icon.
	 * 
	 * <p>
	 * For example, 32 represents a 32x32 icon.
	 * 
	 * @see "The size attribute of the Icon element of a Meta Type Resource."
	 */
	int size();
}
