package test.annotationheaders.custom.metashort;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		short value() default 100;

		short[] array() default {
			120, 15
		};
	}
}
