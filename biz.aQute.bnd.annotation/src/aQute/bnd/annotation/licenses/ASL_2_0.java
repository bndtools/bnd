package aQute.bnd.annotation.licenses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate that the type depends on the Apache License 2.0.
 * Applying this annotation will add a Bundle-License clause.
 *
 * @deprecated Replaced by {@link Apache_2_0}.
 */
@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
@Apache_2_0
public @interface ASL_2_0 {}
