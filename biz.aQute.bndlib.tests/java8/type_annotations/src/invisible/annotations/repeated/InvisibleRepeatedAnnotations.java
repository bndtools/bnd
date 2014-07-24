package invisible.annotations.repeated;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import invisible.annotations.InvisibleRepeatedAnnotation;

@Retention(RetentionPolicy.CLASS)
public @interface InvisibleRepeatedAnnotations {
	InvisibleRepeatedAnnotation[] value();
}
