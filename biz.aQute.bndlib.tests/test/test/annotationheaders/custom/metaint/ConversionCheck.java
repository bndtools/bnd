package test.annotationheaders.custom.metaint;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		int value() default 100;

		int[] array() default {
			120, 15
		};
	}
}
