package test.annotationheaders.custom;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Capability;

@Retention(CLASS)
@Target({
	PACKAGE, TYPE
})
@Repeatable(CustomBs.class)
@Capability(attribute = {
	"osgi.serviceloader=${#value}", "register:=${#register}", "${sjoin;\\;;${#serviceProperty}}"
}, namespace = "osgi.serviceloader", version = "1.0.0")
public @interface CustomB {
	/**
	 * The <em>type</em> of the service.
	 */
	Class<?> value();

	/**
	 * The <em>implementation type</em> of the service. When not specified, uses
	 * the type on which the {@code @ServiceProvider} annotation is placed.
	 */
	Class<?> register() default Target.class;

	/**
	 * A list of service property names and values.
	 * <p>
	 * Each string should be specified in the form:
	 * <ul>
	 * <li>{@code "name=value"} for attributes.</li>
	 * <li>{@code "name:type=value"} for typed attributes.</li>
	 * <li>{@code "name:=value"} for directives.</li>
	 * </ul>
	 * These are added, separated by semicolons, to the export package clause.
	 */
	String[] serviceProperty() default {};
}
