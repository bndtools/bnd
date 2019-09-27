package aQute.bnd.annotation.baseline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Baseline ignore annotation.
 * <p>
 * When this annotation is applied to a baselined element, the baseliner will
 * ignore the annotated element when baselining against a baseline package whose
 * version is less than the specified version. This means the annotated element
 * will not produce a baselining mismatch. The correct baseline information
 * about the element will be in the baseline report, but the element will not
 * cause baselining to fail. When baselining against a baseline package whose
 * version is greater than or equal to the specified version, this annotation is
 * ignored and the annotated element will be included in the baselining.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR
})
public @interface BaselineIgnore {
	/**
	 * Baseline package version.
	 * <p>
	 * The version must be a valid OSGi version string.
	 */
	String value();
}
