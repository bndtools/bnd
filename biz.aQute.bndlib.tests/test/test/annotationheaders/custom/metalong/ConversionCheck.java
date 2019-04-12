package test.annotationheaders.custom.metalong;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		long value() default 100;

		long[] array() default {
			120, 15
		};
	}
}
