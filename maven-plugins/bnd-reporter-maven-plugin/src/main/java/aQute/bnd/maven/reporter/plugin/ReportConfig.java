package aQute.bnd.maven.reporter.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.header.OSGiHeader;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

/**
 * ReportConfig Mojo configuration.
 */
public class ReportConfig {

	/**
	 * Indicates to not include default report plugins.
	 */
	private boolean				clearDefaults	= false;

	/**
	 * A list of additional report plugins.
	 */
	private List<ReportPlugin>	reportPlugins	= new ArrayList<>();

	/**
	 * An arbitrary map of variable which will be added to the report entries.
	 */
	private Map<String, String>	variables		= new HashMap<>();

	public boolean isClearDefaults() {
		return clearDefaults;
	}

	public void setClearDefaults(boolean clearDefaults) {
		this.clearDefaults = clearDefaults;
	}

	public List<ReportPlugin> getReportPlugins() {
		return reportPlugins;
	}

	public void setReportPlugins(List<ReportPlugin> reportPlugins) {
		this.reportPlugins = reportPlugins;
	}

	public Map<String, String> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, String> variables) {
		this.variables = variables;
	}

	/**
	 * Convert this Mojo configuration into a bnd instruction.
	 *
	 * @return the bnd instruction content
	 * @throws MojoExecutionException if a mandatory field is missing
	 */
	public String toInstruction() throws MojoExecutionException {
		StringBuilder result = new StringBuilder();

		if (isClearDefaults()) {
			result.append("clearDefaults,");
		}

		if (getVariables() != null) {
			getVariables().forEach((k, v) -> {
				result.append(EntryNamesReference.ANY_ENTRY)
					.append(";key=")
					.append(k)
					.append(";value=");

				OSGiHeader.quote(result, v != null ? v : "");

				result.append(',');
			});
		}

		for (ReportPlugin plugin : getReportPlugins()) {
			String pluginName = plugin.getPluginName();
			if (StringUtils.isBlank(pluginName)) {
				throw new MojoExecutionException("Missing report plugin name.");
			}

			result.append(pluginName)
				.append(';');

			plugin.getProperties()
				.forEach((key, value) -> {
					result.append(key)
						.append('=');
					OSGiHeader.quote(result, value);
					result.append(';');
				});

			result.setCharAt(result.length() - 1, ',');
		}

		int last = result.length() - 1;
		if (last >= 0 && result.charAt(last) == ',') {
			result.setLength(last);
		}

		return result.toString();
	}
}
