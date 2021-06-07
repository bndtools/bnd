package aQute.bnd.help;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The purpose of this class is to annotate methods in interfaces. Through a
 * proxy, these interfaces can then be used to access the instructions and
 * clauses in a Processor.
 * <p>
 * There are the following types of instructions:
 * <ul>
 * <li>Typed Parameters - Return is a Map<String,T>, where T is an annotated
 * attrs interface. This cannot be optional, it will always return a map,
 * potentially empty.
 * <li>Typed Attrs – Return type is a T, where T is not an iterable but yet an
 * interface annotated with this annotation. This cannot be an Optional.
 * <li>Attrs – Return type is Attrs. This cannot be an Optional
 * <li>Parameters – Return type is Parameters. This cannot be an optional.
 * <li>Lists – Return type is assignable to an iterable. This cannot be an
 * Optional.
 * <li>Basic value – Anything else, is handled by the converter. This can be an
 * Optional.
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
	ElementType.METHOD, ElementType.FIELD
})
public @interface SyntaxAnnotation {

	/**
	 * The property name of the instruction in an Attrs.
	 */
	String name() default "";

	/**
	 * An example of this property
	 */
	String example() default "";

	/**
	 * A lead text about this property
	 */
	String lead() default "";

	/**
	 * A pattern for the key of this parameter
	 */
	String pattern() default "";

}
