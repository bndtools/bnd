package aQute.bnd.annotation.headers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Bundle-Copyright header contains the copyright specification for this
 * bundle.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface BundleCopyright {
	/**
	 * The Bundle-Copyright header contains the copyright specification for this
	 * bundle.
	 */
	String value();
}
