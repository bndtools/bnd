package test.annotations.diff;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Inner {
	String[]value();
}