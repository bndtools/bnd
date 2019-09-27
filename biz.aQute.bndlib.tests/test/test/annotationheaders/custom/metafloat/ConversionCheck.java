package test.annotationheaders.custom.metafloat;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		float value() default 100;

		float[] array() default {
			120, 15
		};
	}
}
