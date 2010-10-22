package aQute.bnd.annotation.component;

public @interface Attribute {
	class C {}
	
	String name() default "";
	String description() default "";
	String[] options();
	
}
