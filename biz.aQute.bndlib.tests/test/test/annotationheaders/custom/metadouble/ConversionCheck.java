package test.annotationheaders.custom.metadouble;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		double value() default 100;

		double[] array() default {
			120, 15
		};
	}
}
