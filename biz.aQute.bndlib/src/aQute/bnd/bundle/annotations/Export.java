/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
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

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.annotation.versioning.Version;

/**
 * Mark a package to be exported from its bundle.
 * <p>
 * The package must also be annotation with the {@link Version} annotation to
 * specify the export version of the package.
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests or otherwise process the package.
 *
 * @author $Id$
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
public @interface Export {
	/**
	 * A list of package names that are used by this package.
	 * <p>
	 * If the {@code uses} directive must be omitted from the export package
	 * clause for this package, the empty value <code>&#x7B;&#x7D;</code> must
	 * be specified.
	 * <p>
	 * If not specified, the {@code uses} directive for the export package
	 * clause is calculated by inspection of the classes in this package.
	 */
	String[] uses() default {};

	/**
	 * A list of attribute or directive names and values.
	 * <p>
	 * Each string should be specified in the form:
	 * <ul>
	 * <li>{@code "name=value"} for attributes.</li>
	 * <li>{@code "name:type=value"} for typed attributes.</li>
	 * <li>{@code "name:=value"} for directives.</li>
	 * </ul>
	 * These are added, separated by semicolons, to the export package clause.
	 */
	String[] attribute() default {};

	/**
	 * Specify the policy for substitutably importing this package.
	 * <p>
	 * Bundles that collaborate require the same class loader for types used in
	 * the collaboration. If multiple bundles export packages with collaboration
	 * types then they will have to be placed in disjoint class spaces, making
	 * collaboration impossible. Collaboration is significantly improved when
	 * bundles are willing to import exported packages; these imports will allow
	 * a framework to substitute exports for imports.
	 * <p>
	 * If not specified, the {@link Substitution#CALCULATED} substitution policy
	 * is used for this package.
	 */
	Substitution substitution() default Substitution.CALCULATED;

	/**
	 * Substitution policy for this package.
	 */
	public enum Substitution {
		/**
		 * Use a consumer type version range for the import package clause when
		 * substitutably importing a package.
		 *
		 * @see ConsumerType
		 */
		CONSUMER,

		/**
		 * Use a provider type version range for the import package clause when
		 * substitutably importing a package.
		 *
		 * @see ProviderType
		 */
		PROVIDER,

		/**
		 * The package must not be substitutably imported.
		 */
		NOIMPORT,

		/**
		 * The policy value is calculated by inspection of the classes in the
		 * package.
		 */
		CALCULATED
	}
}
