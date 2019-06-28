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
 * Define a requirement for a bundle.
 * <p>
 * For example:
 *
 * <pre>
 * &#64;Requirement(namespace=ExtenderNamespace.EXTENDER_NAMESPACE,
 *              name="osgi.component", version="1.3.0")
 * </pre>
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests or otherwise process the a package.
 * <p>
 * This annotation can be used to annotate an annotation.
 *
 * @author $Id$
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.TYPE, ElementType.PACKAGE
})
@Repeatable(Requirements.class)
public @interface Requirement {

	/**
	 * The namespace of this requirement.
	 */
	String namespace();

	/**
	 * The name of this requirement within the namespace.
	 * <p>
	 * If specified, adds an expression, using the {@code &} operator with any
	 * specified {@link #filter()}, to the requirement's filter directive to
	 * test that an attribute with the name of the namespace is equal to the
	 * value of the specified name.
	 */
	String name() default "";

	/**
	 * The floor version of the version range for this requirement.
	 * <p>
	 * If specified, adds a version range expression, using the {@code &}
	 * operator with any specified {@link #filter()}, to the requirement's
	 * filter directive. The ceiling version of the version range is the next
	 * major version from the floor version. For example, if the specified
	 * version is {@code 1.3}, then the version range expression is
	 * {@code (&(version>=1.3)(!(version>=2.0)))}.
	 * <p>
	 * The specified version must be a valid OSGi version string.
	 */
	String version() default "";

	/**
	 * The filter expression of this requirement, if any.
	 */
	String filter() default "";

	/**
	 * The effective time of this requirement.
	 * <p>
	 * Specifies the time the requirement is available. The OSGi framework
	 * resolver only considers requirement without an effective directive or
	 * effective:=resolve. Requirements with other values for the effective
	 * directive can be considered by an external agent.
	 * <p>
	 * If not specified, the {@code effective} directive is omitted from the
	 * requirement clause.
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
	 * These are added, separated by semicolons, to the requirement clause.
	 */
	String[] attribute() default {};

	/**
	 * The cardinality of this requirement.
	 * <p>
	 * Indicates if this requirement can be wired a single time or multiple
	 * times.
	 * <p>
	 * If not specified, the {@code cardinality} directive is omitted from the
	 * requirement clause.
	 */
	Cardinality cardinality() default Cardinality.SINGLE;

	/**
	 * Cardinality for this requirement.
	 */
	public enum Cardinality {
		/**
		 * Indicates if the requirement can only be wired a single time.
		 */
		SINGLE("single"), // Namespace.CARDINALITY_SINGLE

		/**
		 * Indicates if the requirement can be wired multiple times.
		 */
		MULTIPLE("multiple"); // Namespace.CARDINALITY_MULTIPLE

		private final String value;

		Cardinality(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	/**
	 * The resolution policy of this requirement.
	 * <p>
	 * A mandatory requirement forbids the bundle to resolve when this
	 * requirement is not satisfied; an optional requirement allows a bundle to
	 * resolve even if this requirement is not satisfied.
	 * <p>
	 * If not specified, the {@code resolution} directive is omitted from the
	 * requirement clause.
	 */
	Resolution resolution() default Resolution.MANDATORY;

	/**
	 * Resolution for this requirement.
	 */
	public enum Resolution {
		/**
		 * A mandatory requirement forbids the bundle to resolve when the
		 * requirement is not satisfied.
		 */
		MANDATORY("mandatory"), // Namespace.RESOLUTION_MANDATORY

		/**
		 * An optional requirement allows a bundle to resolve even if the
		 * requirement is not satisfied.
		 */
		OPTIONAL("optional"); // Namespace.RESOLUTION_OPTIONAL

		private final String value;

		Resolution(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
