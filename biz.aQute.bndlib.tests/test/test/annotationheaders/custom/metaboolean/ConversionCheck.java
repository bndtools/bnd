package test.annotationheaders.custom.metaboolean;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		boolean value() default false;

		boolean[] array() default {
			true, false
		};
	}
}
