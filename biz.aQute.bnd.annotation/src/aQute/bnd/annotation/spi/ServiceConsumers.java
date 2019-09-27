package aQute.bnd.annotation.spi;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target({
	PACKAGE, TYPE
})
public @interface ServiceConsumers {
	ServiceConsumer[] value();
}
