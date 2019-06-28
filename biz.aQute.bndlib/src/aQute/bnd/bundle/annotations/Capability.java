/*
 * Copyright (c) OSGi Alliance (2016, 2018). All Rights Reserved.
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
 * Define a capability for a bundle.
 * <p>
 * For example:
 *
 * <pre>
 * &#64;Capability(namespace=ExtenderNamespace.EXTENDER_NAMESPACE,
 *             name="osgi.component", version="1.3.0")
 * </pre>
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests or otherwise process the type or package.
 * <p>
 * This annotation can be used to annotate an annotation
 *
 * @author $Id$
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.TYPE, ElementType.PACKAGE
})
@Repeatable(Capabilities.class)
public @interface Capability {

	/**
	 * The namespace of this capability.
	 */
	String namespace();

	/**
	 * The name of this capability within the namespace.
	 * <p>
	 * If specified, adds an attribute with the name of the namespace and the
	 * value of the specified name to the capability clause.
	 */
	String name() default "";

	/**
	 * The version of this capability.
	 * <p>
	 * If specified, adds an attribute with the name and type of
	 * {@code version:Version} and the value of the specified version to the
	 * capability clause.
	 * <p>
	 * The specified version must be a valid OSGi version string.
	 */
	String version() default "";

	/**
	 * A list of classes whose packages are inspected to calculate the
	 * {@code uses} directive for this capability.
	 * <p>
	 * If not specified, the {@code uses} directive is omitted from the
	 * capability clause.
	 */
	Class<?>[] uses() default {};

	/**
	 * The effective time of this capability.
	 * <p>
	 * Specifies the time the capability is available. The OSGi framework
	 * resolver only considers capabilities without an effective directive or
	 * effective:=resolve. Capabilities with other values for the effective
	 * directive can be considered by an external agent.
	 * <p>
	 * If not specified, the {@code effective} directive is omitted from the
	 * capability clause.
	 */
	String effective() default "resolve"; // Namespace.EFFECTIVE_RESOLVE

	/**
	 * A list of attribute or directive names and values.
	 * <p>
	 * Each string should be specified in the form:
	 * <ul>
	 * <li>{@code "name=value"} for attributes.</li>
	 * <li>{@code "name:type=value"} for typed attributes.</li>
	 * <li>{@code "name:=value"} for directives.</li>
	 * </ul>
	 * These are added, separated by semicolons, to the capability clause.
	 */
	String[] attribute() default {};
}
