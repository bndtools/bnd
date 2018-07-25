package aQute.bnd.annotation.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Reference {
	String	RNAME		= "LaQute/bnd/annotation/component/Reference;";
	String	NAME		= "name";
	String	SERVICE		= "service";
	String	OPTIONAL	= "optional";
	String	MULTIPLE	= "multiple";
	String	DYNAMIC		= "dynamic";
	String	TARGET		= "target";
	String	TYPE		= "type";
	String	UNBIND		= "unbind";

	String name() default "";

	Class<?> service() default Object.class;

	boolean optional() default false;

	boolean multiple() default false;

	boolean dynamic() default false;

	String target() default "";

	String unbind() default "";

	char type() default 0;
}
