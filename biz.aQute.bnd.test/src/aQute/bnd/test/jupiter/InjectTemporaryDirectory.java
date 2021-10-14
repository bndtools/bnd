package aQute.bnd.test.jupiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target({
	ElementType.FIELD, ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TemporaryDirectoryExtension.class)
public @interface InjectTemporaryDirectory {
	String value() default "generated/tmp/test";

	boolean clear() default true;
}
