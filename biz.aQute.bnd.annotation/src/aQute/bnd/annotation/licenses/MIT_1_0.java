package aQute.bnd.annotation.licenses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate that the type depends on the MIT License. Applying
 * this annotation will add a Bundle-License clause.
 *
 * @deprecated Replaced by {@link MIT}.
 */
@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
@MIT
public @interface MIT_1_0 {}
