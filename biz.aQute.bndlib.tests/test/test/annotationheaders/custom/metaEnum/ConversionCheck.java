package test.annotationheaders.custom.metaEnum;

import org.osgi.annotation.bundle.Capability;

@ConversionCheck.Meta
public class ConversionCheck {
	@Capability(attribute = {
		"value=${#value}", "array='${#array}'"
	}, namespace = "type")
	public static @interface Meta {
		Foo value() default Foo.BAR;

		Foo[] array() default {
			Foo.BAZ, Foo.FOO
		};
	}

	public enum Foo {
		FOO,
		BAR,
		BAZ
	}
}
