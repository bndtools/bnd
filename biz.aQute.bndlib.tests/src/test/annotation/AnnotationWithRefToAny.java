package test.annotation;

import java.lang.annotation.*;

import test.annotation.any.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationWithRefToAny {
	Class<?> c() default Any.class;
}
