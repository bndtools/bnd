package aQute.lib.getopt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotated command method says that is has sub-commands specified by the
 * given class in {@link #value()}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
	ElementType.METHOD
})
public @interface SubCommands {
	/**
	 * @return the class containing sub-commands for a command.
	 */
	Class<?> value();
}
