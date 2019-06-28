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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark an annotation element as an attribute.
 * <p>
 * This is used when applying {@link Capability} or {@link Requirement} as a
 * meta annotation to an annotation declaration. The value of the annotation
 * element annotated with {@code Attribute} is used as the value of an attribute
 * in the generated capability or requirement clause. For example:
 *
 * <pre>
 * &#64;Capability(namespace = "my.namespace")
 * public &#64;interface MyCapability {
 *   &#64;Attribute("attr")
 *   String value() default "";
 * }
 *
 * &#64;MyCapability("foo")
 * public MyClass {}
 * </pre>
 *
 * The use of the {@code MyCapability} annotation, which is meta annotated with
 * the {@code Capability} and {@code Attribute} annotations, will result in a
 * capability in the namespace {@code my.namespace} with the attribute
 * {@code attr=foo}.
 * <p>
 * If the element annotated with {@code Attribute} is unspecified when applied,
 * then the attribute must not be generated in the generated capability or
 * requirement clause. For example:
 *
 * <pre>
 * &#64;MyCapability
 * public MyClass {}
 * </pre>
 *
 * will not have the {@code attr} attribute in the generated capability.
 * <p>
 * This annotation is not retained at runtime. It is for use by tools to
 * generate bundle manifests.
 *
 * @author $Id$
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Attribute {
	/**
	 * The name of the attribute.
	 * <p>
	 * If not specified, the name of the annotated element is used as the name
	 * of the attribute.
	 */
	String value() default "";
}
