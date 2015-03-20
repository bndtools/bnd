package aQute.bnd.annotation.xml;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.TYPE, ElementType.METHOD
})
public @interface Attributes {
	
	Attribute[] value();

}
