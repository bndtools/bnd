package aQute.bnd.annotation.plugin;

import java.util.Optional;

/**
 * This type represents a detected Internal Plugin as defined by the
 * {@link BndPlugin} annotation (and {@link InternalPluginNamespace}
 * capability.)
 */
public interface InternalPluginDefinition {
	/**
	 * The short name of the plugin as set with the {@link BndPlugin#name()}
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * The implementation type of the plugin as set with the type that the
	 * {@link BndPlugin} annotation is applied to.
	 *
	 * @return the implementation class
	 */
	Class<?> getImplementation();

	/**
	 * The configuration type of the plugin as set with
	 * {@link BndPlugin#parameters()}. The name is not configuration sadly due
	 * to baselining.
	 *
	 * @return the configuration class
	 */
	Optional<Class<?>> getParameters();

	/**
	 * A template string for this plugin including all attributes the plugin
	 * supports
	 * 
	 * @return a template definition for this plugin
	 */
	String getTemplate();

	/**
	 * If this plugin should be hidden
	 * 
	 * @return true if this plugin should be hidden
	 */
	boolean isHidden();
}
