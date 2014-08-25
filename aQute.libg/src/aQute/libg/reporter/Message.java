package aQute.libg.reporter;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Message {
	String value();
}
