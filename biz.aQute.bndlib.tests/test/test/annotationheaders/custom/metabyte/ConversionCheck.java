package test.annotationheaders.custom.metabyte;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		byte value() default 100;

		byte[] array() default {
			120, 15
		};
	}
}
