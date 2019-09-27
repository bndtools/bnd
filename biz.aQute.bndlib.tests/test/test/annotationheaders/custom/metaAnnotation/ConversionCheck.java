package test.annotationheaders.custom.metaAnnotation;

import org.osgi.annotation.bundle.Capability;

@Meta
public class ConversionCheck {}

@Capability(attribute = {
	"value=${#value}", "array='${#array}'"
}, namespace = "type")
@interface Meta {
	Foo value() default @Foo;

	Foo[] array() default {
		@Foo(2), @Foo(value = 4, bars = @Bar("test"))
	};
}

@interface Foo {
	int value() default 1;

	Bar[] bars() default {};
}

@interface Bar {
	String value();
}
