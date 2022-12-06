package aQute.bnd.maven.reporter.plugin;

import java.util.Map;

/**
 * ReportPlugin Mojo configuration.
 */
public class ReportPlugin {

	/**
	 * The name or the canonical class name of the report plugin (mandatory).
	 */
	private String				pluginName;

	/**
	 * A map of the plugin properties.
	 */
	private Map<String, String>	properties;

	public String getPluginName() {
		return pluginName;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
}
