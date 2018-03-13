package test.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import test.annotation.any.Any;

@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationWithRefToAny {
	Class<?> c() default Any.class;
}
