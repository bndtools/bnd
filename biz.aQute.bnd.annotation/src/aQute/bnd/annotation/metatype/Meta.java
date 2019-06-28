package aQute.bnd.annotation.metatype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Metadata interface provides access to the properties that underly a
 * Configurable interface. Any Configurable interface can implement this
 * interface. The interface provides the annotations that can be used to create
 * metatype objects. @ConsumerInterface
 */

public interface Meta {
	enum Type {
		Boolean,
		Byte,
		Character,
		Short,
		Integer,
		Long,
		Float,
		Double,
		String,
		Password
	}

	/**
	 * Constant NULL for default usage
	 */
	String NULL = "§NULL§";

	/**
	 * The OCD Annotation maps to the OCD element in the Metatype specification.
	 * The only difference is that it is possible to create a Designate element
	 * as well.
	 */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface OCD {
		/**
		 * The name for this component. The default name is a the short class
		 * name that us un-camel cased to make it more readable.
		 *
		 * @return The name of this component
		 */
		String name() default NULL;

		/**
		 * The id of the component. Default the name of the class in FQN
		 * notation but with nested classes using the $ as separator (not .).
		 * The Felix webconsole always uses this id as the PID and not the pid
		 * in the Designate element. Reported as an error.
		 *
		 * @return the id
		 */
		String id() default NULL;

		/**
		 * The localization prefix. The default localization prefix is the name
		 * of the class with a $ separator for nested classes.
		 *
		 * @return the localization prefix.
		 */
		String localization() default NULL;

		/**
		 * A description for this ocd. The default is empty.
		 *
		 * @return the description
		 */
		String description() default NULL;

		/**
		 * Defines if this is for a factory or not.
		 */
		boolean factory() default false;
	}

	/**
	 * The AD element in the Metatype specification.
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface AD {
		/**
		 * A description of the attribute. Default is empty.
		 *
		 * @return The description of the attribute.
		 */
		String description() default NULL;

		/**
		 * The name of the attribute. By default the un-camel cased version of
		 * the method name.
		 *
		 * @return the name
		 */
		String name() default NULL;

		/**
		 * The id of the attribute. By default the name of the method. The id is
		 * the key used to access the properties. This is the reason the AD is a
		 * runtime annotation so the runtime can find the proper key.
		 *
		 * @return the id
		 */
		String id() default NULL;

		/**
		 * The type of the field. This must be one of the basic types in the
		 * metatype specification. By default, the type is derived from the
		 * return type of the method. This includes most collections and arrays.
		 * Unrecognized types are defaulted to String.
		 *
		 * @return the type to be used.
		 */
		Type type() default Type.String;

		/**
		 * The cardinality of the attribute. If not explicitly set it will be
		 * derived from the attributes return type. Collections return
		 * Integer.MIN_VALUE and arrays use Integer.MAX_VALUE. If a single
		 * string needs to be converted to a Collection or array then the | will
		 * be used as a separator to split the line.
		 *
		 * @return the cardinality of the attribute
		 */
		int cardinality() default 0;

		/**
		 * The minimum value. This string must be converted to the attribute
		 * type before comparison takes place.
		 *
		 * @return the min value
		 */
		String min() default NULL;

		/**
		 * The maximum value. This string must be converted to the attribute
		 * type before comparison takes place.
		 *
		 * @return the max value
		 */
		String max() default NULL;

		/**
		 * The default value. This value must be converted to the return type of
		 * the attribute. For multi valued returns use the | as separator.
		 *
		 * @return the default value
		 */
		String deflt() default NULL;

		/**
		 * Indicates that this attribute is required. By default attributes are
		 * required.
		 */
		boolean required() default true;

		/**
		 * Provide labels for options. These labels must match the values. If no
		 * labels are set, the un-cameled version of the values are used (if
		 * they are set of course).
		 *
		 * @return the option labels
		 */
		String[] optionLabels() default NULL;

		/**
		 * The values of options. If not set and the return type is an enum
		 * class then the values will be derived from this return type.
		 *
		 * @return the option labels
		 */
		String[] optionValues() default NULL;
	}
}
