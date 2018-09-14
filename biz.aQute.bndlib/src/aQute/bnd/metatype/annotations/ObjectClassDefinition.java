/*
 * Copyright (c) OSGi Alliance (2013, 2018). All Rights Reserved.
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

import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirement.Resolution;
import org.osgi.namespace.extender.ExtenderNamespace;

/**
 * Generate a Meta Type Resource using the annotated type.
 * <p>
 * This annotation can be used without defining any element values since
 * defaults can be generated from the annotated type. Each method of the
 * annotated type has an implied {@link AttributeDefinition} annotation if not
 * explicitly annotated.
 * <p>
 * This annotation may only be used on annotation types and interface types. Use
 * on concrete or abstract class types is unsupported. If applied to an
 * interface then all methods inherited from super types are included as
 * attributes.
 * <p>
 * This annotation is not processed at runtime. It must be processed by tools
 * and used to generate a Meta Type Resource document for the bundle.
 * 
 * @see "The OCD element of a Meta Type Resource."
 * @author $Id$
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Requirement(namespace = ExtenderNamespace.EXTENDER_NAMESPACE, //
	name = MetaTypeConstants.METATYPE_CAPABILITY_NAME, //
	version = MetaTypeConstants.METATYPE_SPECIFICATION_VERSION, //
		resolution = Resolution.OPTIONAL)
public @interface ObjectClassDefinition {
	/**
	 * The id of this ObjectClassDefinition.
	 * 
	 * <p>
	 * If not specified, the id of this ObjectClassDefinition is the fully
	 * qualified name of the annotated type using the dollar sign ({@code '$'}
	 * &#92;u0024) to separate nested class names from the name of their
	 * enclosing class. The id is not to be confused with a PID which can be
	 * specified by the {@link #pid()} or {@link #factoryPid()} element.
	 * 
	 * @see "The id attribute of the OCD element of a Meta Type Resource."
	 */
	String id() default "";

	/**
	 * The human readable name of this ObjectClassDefinition.
	 * 
	 * <p>
	 * If not specified, the name of this ObjectClassDefinition is derived from
	 * the {@link #id()}. For example, low line ({@code '_'} &#92;u005F) and
	 * dollar sign ({@code '$'} &#92;u0024) are replaced with space ({@code ' '}
	 * &#92;u0020) and space is inserted between camel case words.
	 * 
	 * <p>
	 * If the name begins with the percent sign ({@code '%'} &#92;u0025), the
	 * name can be {@link #localization() localized}.
	 * 
	 * @see "The name attribute of the OCD element of a Meta Type Resource."
	 */
	String name() default "";

	/**
	 * The human readable description of this ObjectClassDefinition.
	 * 
	 * <p>
	 * If not specified, the description of this ObjectClassDefinition is the
	 * empty string.
	 * 
	 * <p>
	 * If the description begins with the percent sign ({@code '%'} &#92;u0025),
	 * the description can be {@link #localization() localized}.
	 * 
	 * @see "The description attribute of the OCD element of a Meta Type Resource."
	 */
	String description() default "";

	/**
	 * The localization resource of this ObjectClassDefinition.
	 * 
	 * <p>
	 * This refers to a resource property entry in the bundle that can be
	 * augmented with locale information. If not specified, the localization
	 * resource for this ObjectClassDefinition is the string
	 * &quot;OSGI-INF/l10n/&quot; followed by the {@link #id()}.
	 * 
	 * @see "The localization attribute of the MetaData element of a Meta Type Resource."
	 */
	String localization() default "";

	/**
	 * The PIDs associated with this ObjectClassDefinition.
	 * <p>
	 * For each specified PID, a {@code Designate} element with a pid attribute
	 * is generated that {@link #id() references} this ObjectClassDefinition.
	 * <p>
	 * The {@link Designate} annotation can also be used to associate a
	 * Declarative Services component with an ObjectClassDefinition and generate
	 * a {@code Designate} element.
	 * <p>
	 * A special string ({@code "$"}) can be used to specify the fully qualified
	 * name of the annotated type as a PID. For example:
	 * 
	 * <pre>
	 * &#64;ObjectClassDefinition(pid="$")
	 * </pre>
	 * 
	 * Tools creating a Meta Type Resource from this annotation must replace the
	 * special string with the fully qualified name of the annotated type.
	 * 
	 * @see "The pid attribute of the Designate element of a Meta Type Resource."
	 * @see Designate
	 */
	String[] pid() default {};

	/**
	 * The factory PIDs associated with this ObjectClassDefinition.
	 * <p>
	 * For each specified factory PID, a {@code Designate} element with a
	 * factoryPid attribute is generated that {@link #id() references} this
	 * ObjectClassDefinition.
	 * <p>
	 * The {@link Designate} annotation can also be used to associate a
	 * Declarative Services component with an ObjectClassDefinition and generate
	 * a {@code Designate} element.
	 * <p>
	 * A special string ({@code "$"}) can be used to specify the fully qualified
	 * name of the annotated type as a factory PID. For example:
	 * 
	 * <pre>
	 * &#64;ObjectClassDefinition(factoryPid="$")
	 * </pre>
	 * 
	 * Tools creating a Meta Type Resource from this annotation must replace the
	 * special string with the fully qualified name of the annotated type.
	 * 
	 * @see "The factoryPid attribute of the Designate element of a Meta Type Resource."
	 * @see Designate
	 */
	String[] factoryPid() default {};

	/**
	 * The icon resources associated with this ObjectClassDefinition.
	 * 
	 * <p>
	 * For each specified {@link Icon}, an {@code Icon} element is generated for
	 * this ObjectClassDefinition. If not specified, no {@code Icon} elements
	 * will be generated.
	 * 
	 * @see "The Icon element of a Meta Type Resource."
	 */
	Icon[] icon() default {};
}
