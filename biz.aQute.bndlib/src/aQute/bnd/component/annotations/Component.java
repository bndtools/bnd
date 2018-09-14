/*
 * Copyright (c) OSGi Alliance (2011, 2018). All Rights Reserved.
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
 * Identify the annotated class as a Service Component.
 * 
 * <p>
 * The annotated class is the implementation class of the Component.
 * 
 * <p>
 * This annotation is not processed at runtime by Service Component Runtime. It
 * must be processed by tools and used to add a Component Description to the
 * bundle.
 * 
 * @see "The component element of a Component Description."
 * @author $Id$
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@RequireServiceComponentRuntime
public @interface Component {
	/**
	 * The name of this Component.
	 * 
	 * <p>
	 * If not specified, the name of this Component is the fully qualified type
	 * name of the class being annotated.
	 * 
	 * @see "The name attribute of the component element of a Component Description."
	 */
	String name() default "";

	/**
	 * The types under which to register this Component as a service.
	 * 
	 * <p>
	 * If no service should be registered, the empty value
	 * <code>&#x7B;&#x7D;</code> must be specified.
	 * 
	 * <p>
	 * If not specified, the service types for this Component are all the
	 * <i>directly</i> implemented interfaces of the class being annotated.
	 * 
	 * @see "The service element of a Component Description."
	 */
	Class<?>[] service() default {};

	/**
	 * The factory identifier of this Component. Specifying a factory identifier
	 * makes this Component a Factory Component.
	 * 
	 * <p>
	 * If not specified, the default is that this Component is not a Factory
	 * Component.
	 * 
	 * @see "The factory attribute of the component element of a Component Description."
	 */
	String factory() default "";

	/**
	 * Declares whether this Component uses the OSGi ServiceFactory concept and
	 * each bundle using this Component's service will receive a different
	 * component instance.
	 * 
	 * <p>
	 * This element is ignored when the {@link #scope()} element does not have
	 * the default value. If {@code true}, this Component uses
	 * {@link ServiceScope#BUNDLE bundle} service scope. If {@code false} or not
	 * specified, this Component uses {@link ServiceScope#SINGLETON singleton}
	 * service scope. If the {@link #factory()} element is specified or the
	 * {@link #immediate()} element is specified with {@code true}, this element
	 * can only be specified with {@code false}.
	 * 
	 * @see "The scope attribute of the service element of a Component Description."
	 * @deprecated Since 1.3. Replaced by {@link #scope()}.
	 */
	boolean servicefactory() default false;

	/**
	 * Declares whether this Component is enabled when the bundle declaring it
	 * is started.
	 * <p>
	 * If {@code true} or not specified, this Component is enabled. If
	 * {@code false}, this Component is disabled.
	 * 
	 * @see "The enabled attribute of the component element of a Component Description."
	 */
	boolean enabled() default true;

	/**
	 * Declares whether this Component must be immediately activated upon
	 * becoming satisfied or whether activation should be delayed.
	 * 
	 * <p>
	 * If {@code true}, this Component must be immediately activated upon
	 * becoming satisfied. If {@code false}, activation of this Component is
	 * delayed. If this property is specified, its value must be {@code false}
	 * if the {@link #factory()} property is also specified or must be
	 * {@code true} if the {@link #service()} property is specified with an
	 * empty value.
	 * 
	 * <p>
	 * If not specified, the default is {@code false} if the {@link #factory()}
	 * property is specified or the {@link #service()} property is not specified
	 * or specified with a non-empty value and {@code true} otherwise.
	 * 
	 * @see "The immediate attribute of the component element of a Component Description."
	 */
	boolean immediate() default false;

	/**
	 * Properties for this Component.
	 * <p>
	 * Each property string is specified as {@code "name=value"}. The type of
	 * the property value can be specified in the name as
	 * {@code name:type=value}. The type must be one of the property types
	 * supported by the {@code type} attribute of the {@code property} element
	 * of a Component Description.
	 * <p>
	 * To specify a property with multiple values, use multiple name, value
	 * pairs. For example, <code>{"foo=bar", "foo=baz"}</code>.
	 * 
	 * @see "The property element of a Component Description."
	 */
	String[] property() default {};

	/**
	 * Property entries for this Component.
	 * 
	 * <p>
	 * Specifies the name of an entry in the bundle whose contents conform to a
	 * standard Java Properties File. The entry is read and processed to obtain
	 * the properties and their values.
	 * 
	 * @see "The properties element of a Component Description."
	 */
	String[] properties() default {};

	/**
	 * The XML name space of the Component Description for this Component.
	 * 
	 * <p>
	 * If not specified, the XML name space of the Component Description for
	 * this Component should be the lowest Declarative Services XML name space
	 * which supports all the specification features used by this Component.
	 * 
	 * @see "The XML name space specified for a Component Description."
	 */
	String xmlns() default "";

	/**
	 * The configuration policy of this Component.
	 * 
	 * <p>
	 * Controls whether component configurations must be satisfied depending on
	 * the presence of a corresponding Configuration object in the OSGi
	 * Configuration Admin service. A corresponding configuration is a
	 * Configuration object where the PID equals the name of the component.
	 * 
	 * <p>
	 * If not specified, the configuration policy is based upon whether the
	 * component is also annotated with the Meta Type
	 * {@link aQute.bnd.metatype.annotations.Designate Designate} annotation.
	 * <ul>
	 * <li>Not annotated with {@code Designate} - The configuration policy is
	 * {@link ConfigurationPolicy#OPTIONAL OPTIONAL}.</li>
	 * <li>Annotated with {@code Designate(factory=false)} - The configuration
	 * policy is {@link ConfigurationPolicy#OPTIONAL OPTIONAL}.</li>
	 * <li>Annotated with {@code Designate(factory=true)} - The configuration
	 * policy is {@link ConfigurationPolicy#REQUIRE REQUIRE}.</li>
	 * </ul>
	 * 
	 * @see "The configuration-policy attribute of the component element of a Component Description."
	 * @since 1.1
	 */
	ConfigurationPolicy configurationPolicy() default ConfigurationPolicy.OPTIONAL;

	/**
	 * The configuration PIDs for the configuration of this Component.
	 * <p>
	 * Each value specifies a configuration PID for this Component.
	 * <p>
	 * If no value is specified, the name of this Component is used as the
	 * configuration PID of this Component.
	 * <p>
	 * A special string (<code>"$"</code>) can be used to specify the name of
	 * the component as a configuration PID. The {@link #NAME} constant holds
	 * this special string. For example:
	 * 
	 * <pre>
	 * &#64;Component(configurationPid={"com.acme.system", Component.NAME})
	 * </pre>
	 * 
	 * Tools creating a Component Description from this annotation must replace
	 * the special string with the actual name of this Component.
	 * 
	 * @see "The configuration-pid attribute of the component element of a Component Description."
	 * @since 1.2
	 */
	String[] configurationPid() default NAME;

	/**
	 * Special string representing the name of this Component.
	 * 
	 * <p>
	 * This string can be used in {@link #configurationPid()} to specify the
	 * name of the component as a configuration PID. For example:
	 * 
	 * <pre>
	 * &#64;Component(configurationPid={"com.acme.system", Component.NAME})
	 * </pre>
	 * 
	 * Tools creating a Component Description from this annotation must replace
	 * the special string with the actual name of this Component.
	 * 
	 * @since 1.3
	 */
	String	NAME	= "$";

	/**
	 * The service scope for the service of this Component.
	 * 
	 * <p>
	 * If not specified (and the deprecated {@link #servicefactory()} element is
	 * not specified), the {@link ServiceScope#SINGLETON singleton} service
	 * scope is used. If the {@link #factory()} element is specified or the
	 * {@link #immediate()} element is specified with {@code true}, this element
	 * can only be specified with the {@link ServiceScope#SINGLETON singleton}
	 * service scope.
	 * 
	 * @see "The scope attribute of the service element of a Component Description."
	 * @since 1.3
	 */
	ServiceScope scope() default ServiceScope.DEFAULT;

	/**
	 * The lookup strategy references of this Component.
	 * <p>
	 * To access references using the lookup strategy, {@link Reference}
	 * annotations are specified naming the reference and declaring the type of
	 * the referenced service. The referenced service can be accessed using one
	 * of the {@code locateService} methods of {@code ComponentContext}.
	 * <p>
	 * To access references using method injection, bind methods are annotated
	 * with {@link Reference}. To access references using field injection,
	 * fields are annotated with {@link Reference}. To access references using
	 * constructor injection, constructor parameters are annotated with
	 * {@link Reference}.
	 * 
	 * @see "The reference element of a Component Description."
	 * @since 1.3
	 */
	Reference[] reference() default {};

	/**
	 * Factory properties for this Factory Component.
	 * <p>
	 * Each factory property string is specified as {@code "name=value"}. The
	 * type of the factory property value can be specified in the name as
	 * {@code name:type=value}. The type must be one of the factory property
	 * types supported by the {@code type} attribute of the
	 * {@code factory-property} element of a Component Description.
	 * <p>
	 * To specify a factory property with multiple values, use multiple name,
	 * value pairs. For example, <code>{"foo=bar", "foo=baz"}</code>.
	 * <p>
	 * If specified, the {@link #factory()} element must also be specified to
	 * indicate the component is a Factory Component.
	 * 
	 * @see "The factory-property element of a Component Description."
	 * @since 1.4
	 */
	String[] factoryProperty() default {};

	/**
	 * Factory property entries for this Factory Component.
	 * <p>
	 * Specifies the name of an entry in the bundle whose contents conform to a
	 * standard Java Properties File. The entry is read and processed to obtain
	 * the factory properties and their values.
	 * <p>
	 * If specified, the {@link #factory()} element must also be specified to
	 * indicate the component is a Factory Component.
	 * 
	 * @see "The factory-properties element of a Component Description."
	 * @since 1.4
	 */
	String[] factoryProperties() default {};
}
