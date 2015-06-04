package aQute.bnd.annotation.headers;

import java.lang.annotation.*;

/**
 * Container type for {@link aQute.bnd.annotation.headers.RequireCapability}
 * annotations.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface RequireCapabilities {
	RequireCapability[] value();
}