package test.annotations.diff;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Outer {
	Inner[] value();
	int[] x();
}
