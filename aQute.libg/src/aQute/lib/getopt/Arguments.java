package aQute.lib.getopt;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Arguments {
	String[] arg();
}
