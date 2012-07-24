package aQute.bnd.annotation.component;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Deactivate {
	String	RNAME	= "LaQute/bnd/annotation/component/Deactivate;";

}
