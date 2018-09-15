/*
 * Copyright (c) OSGi Alliance (2014, 2016). All Rights Reserved.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate a {@code Designate} element in the Meta Type Resource for an
 * {@link ObjectClassDefinition} using the annotated Declarative Services
 * component.
 * 
 * <p>
 * This annotation must be used on a type that is also annotated with the
 * Declarative Services {@link aQute.bnd.component.annotations.Component
 * Component} annotation. The component must only have a single PID which is
 * used for the generated {@code Designate} element.
 * 
 * <p>
 * This annotation is not processed at runtime. It must be processed by tools
 * and used to contribute to a Meta Type Resource document for the bundle.
 * 
 * @see "The Designate element of a Meta Type Resource."
 * @author $Id$
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Designate {
	/**
	 * The type of the {@link ObjectClassDefinition} for this Designate.
	 * 
	 * <p>
	 * The specified type must be annotated with {@link ObjectClassDefinition}.
	 * 
	 * @see "The ocdref attribute of the Designate element of a Meta Type Resource."
	 */
	Class<?> ocd();

	/**
	 * Specifies whether this Designate is for a factory PID.
	 * 
	 * <p>
	 * If {@code false}, then the PID value from the annotated component will be
	 * used in the {@code pid} attribute of the generated {@code Designate}
	 * element. If {@code true}, then the PID value from the annotated component
	 * will be used in the {@code factoryPid} attribute of the generated
	 * {@code Designate} element.
	 * 
	 * @see "The pid and factoryPid attributes of the Designate element of a Meta Type Resource."
	 */
	boolean factory() default false;
}
