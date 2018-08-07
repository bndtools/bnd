package aQute.bnd.annotation.headers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This meta-annotation allows the developer to define an annotation that
 * generates an arbitrary manifest header in the output bundle
 * <p>
 * For example given the following annotation and its usage:
 * 
 * <pre>
 * &#64;BundleHeader(name = "My-Bundle-Header")
 * public @interface MyBundleHeader {}
 * 
 * package org.example;
 * &#64;MyBundleHeader
 * public class Example {}
 * </pre>
 * 
 * the following bundle header is generated:
 * 
 * <pre>
 * My-Bundle-Header: org.example.Example
 * </pre>
 * <p>
 * Where an annotation is used on multiple classes within the bundle, the value
 * of each shall be appended as a list into a single manifest header. However,
 * if the annotation attribute {@code singleton} is true then multiple usages of
 * the annotation within a single bundle generates an error.
 * <p>
 * The {@code value} attribute can use the following macro values:
 * <ul>
 * <li>$&#123;&#64;class&#125; adds the FQN of the target type</li>
 * <li>$&#123;&#64;class-simple&#125; adds the short name of the target
 * type</li>
 * <li>$&#123;&#64;package&#125; adds the FQN of the target type's package</li>
 * </ul>
 * The default value is $&#123;&#64;class&#125;.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface BundleHeader {

	/**
	 * The name of the bundle manifest header. Must not be empty and should
	 * begin with an upper-case character (otherwise it will be omitted from the
	 * generated manifest).
	 */
	String name();

	/**
	 * The value of the generated manifest header, which may be parameterized
	 * with macros as described above. Defaults to the FQN of the type to which
	 * the annotation is attached.
	 */
	String value() default "${@class}";

	/**
	 * Whether this header should be a singleton within the scope of the bundle.
	 */
	boolean singleton() default false;

}
