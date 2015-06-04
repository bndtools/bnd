package aQute.bnd.annotation.headers;

import java.lang.annotation.*;

/**
 * Container type for {@link aQute.bnd.annotation.headers.ProvideCapability}
 * annotations.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface ProvideCapabilities {
	ProvideCapability[] value();
}