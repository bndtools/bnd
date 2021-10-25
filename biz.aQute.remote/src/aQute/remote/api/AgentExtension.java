package aQute.remote.api;

import java.util.Map;

import aQute.bnd.annotation.ConsumerType;

/**
 * Service interface to be used by consumers for providing custom
 * functionalities that can be invoked through the bnd agent.
 * <p>
 * The services must provide the following key as a service property.
 */
@ConsumerType
@FunctionalInterface
public interface AgentExtension {

	/** The service property key to be set */
	String PROPERTY_KEY = "agent.extension.name";

	/**
	 * Returns the results as a type supported by the bnd converter
	 *
	 * @param context the context for the extension
	 * @return the result as a bnd converter supported type
	 */
	Object execute(Map<String, Object> context);
}
