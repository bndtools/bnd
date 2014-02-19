package aQute.bnd.annotation.headers;

import java.lang.annotation.*;

/**
 * The Bundle-DocURL headers must contain a URL pointing to documentation about
 * this bundle.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface BundleDocURL {
	/**
	 * The Bundle-DocURL headers must contain a URL pointing to documentation
	 * about this bundle.
	 */
	String value();
}
