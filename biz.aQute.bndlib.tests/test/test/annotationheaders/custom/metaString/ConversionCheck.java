package test.annotationheaders.custom.metaString;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array:List<String>='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		String value() default "green";

		String[] array() default {
			"red", "blue\\,green"
		};
	}
}
