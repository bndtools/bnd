package aQute.bnd.service;

/**
 * A plugin that wants a registry
 */
public interface RegistryPlugin {
	void setRegistry(Registry registry);
}
