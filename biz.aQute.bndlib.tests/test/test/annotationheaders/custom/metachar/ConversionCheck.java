package test.annotationheaders.custom.metachar;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		char value() default 'a';

		char[] array() default {
			'[', ']'
		};
	}
}
