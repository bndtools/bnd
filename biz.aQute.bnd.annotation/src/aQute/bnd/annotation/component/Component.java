package aQute.bnd.annotation.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Component {
	String	RNAME					= "LaQute/bnd/annotation/component/Component;";
	String	PROVIDE					= "provide";
	String	NAME					= "name";
	String	FACTORY					= "factory";
	String	SERVICEFACTORY			= "servicefactory";
	String	IMMEDIATE				= "immediate";
	String	CONFIGURATION_POLICY	= "configurationPolicy";
	String	ENABLED					= "enabled";
	String	PROPERTIES				= "properties";
	String	VERSION					= "version";
	String	DESIGNATE				= "designate";
	String	DESIGNATE_FACTORY		= "designateFactory";

	String name() default "";

	Class<?>[] provide() default Object.class;

	String factory() default "";

	boolean servicefactory() default false;

	boolean enabled() default true;

	boolean immediate() default false;

	/**
	 * @deprecated
	 */
	ConfigurationPolicy configurationPolicy() default ConfigurationPolicy.optional;

	String[] properties() default {};

	Class<?> designate() default Object.class;

	Class<?> designateFactory() default Object.class;
}
