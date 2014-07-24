package runtime.annotations.repeated;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import runtime.annotations.RuntimeRepeatedAnnotation;


@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeRepeatedAnnotations {
	RuntimeRepeatedAnnotation[] value();
}
