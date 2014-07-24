package runtime.annotations;

import java.lang.annotation.Repeatable;

import runtime.annotations.repeated.RuntimeRepeatedAnnotations;

@Repeatable(RuntimeRepeatedAnnotations.class)
public @interface RuntimeRepeatedAnnotation {

}
