package aQute.bnd.annotation.headers;

import java.lang.annotation.*;
/**
 * The Bundle-Copyright header contains the copyright specification for this bundle.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface BundleCopyright {
	/**
	 * The Bundle-Copyright header contains the copyright specification for this bundle.
	 */
	String value();
}
