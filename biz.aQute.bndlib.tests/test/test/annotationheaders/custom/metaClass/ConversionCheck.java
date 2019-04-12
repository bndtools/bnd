package test.annotationheaders.custom.metaClass;

import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'", "host=${#referToHostType}"
	}, namespace = "type")
	public static @interface Meta {
		Class<?> value() default Boolean.class;

		Class<?>[] array() default {
			Long.class, Integer.class, Target.class
		};

		Class<?> referToHostType() default Target.class;
	}
}
