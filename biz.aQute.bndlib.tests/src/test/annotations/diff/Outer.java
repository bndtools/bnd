package test.annotations.diff;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Outer {
	Inner[] value();

	int[] x();
}
