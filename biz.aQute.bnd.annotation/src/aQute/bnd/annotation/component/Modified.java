package aQute.bnd.annotation.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Modified {
	String RNAME = "LaQute/bnd/annotation/component/Modified;";

}
