package aQute.bnd.maven.reporter.plugin;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import aQute.bnd.header.OSGiHeader;

/**
 * Report Mojo configuration.
 */
public class Report {

	/**
	 * Output file path of the report (mandatory).
	 */
	private String				outputFile;

	/**
	 * Path or URL of a template file.
	 */
	private String				templateFile;

	/**
	 * The template type (eg; xslt, twig, ...).
	 */
	private String				templateType;

	/**
	 * A locale for the report (eg; en-US).
	 */
	private String				locale;

	/**
	 * An arbitrary map of template parameters.
	 */
	private Map<String, String>	parameters	= new HashMap<>();

	/**
	 * The name of the configuration to use to generate the report.
	 */
	private String				configName;

	/**
	 * The scope of the report (eg; aggregator, project).
	 */
	private String				scope;

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public String getTemplateFile() {
		return templateFile;
	}

	public void setTemplateFile(String templateFile) {
		this.templateFile = templateFile;
	}

	public String getTemplateType() {
		return templateType;
	}

	public void setTemplateType(String templateType) {
		this.templateType = templateType;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public String getConfigName() {
		return configName;
	}

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	/**
	 * Convert this Mojo configuration into a bnd instruction.
	 *
	 * @return the bnd instruction content
	 * @throws MojoExecutionException if a mandatory field is missing
	 */
	public String toInstruction() throws MojoExecutionException {
		if (StringUtils.isBlank(getOutputFile())) {
			throw new MojoExecutionException("Missing report output file.");
		}

		StringBuilder result = new StringBuilder().append(getOutputFile());

		if (StringUtils.isNotBlank(getTemplateFile())) {
			result.append(";template=")
				.append(getTemplateFile());
		}

		if (StringUtils.isNotBlank(getTemplateType())) {
			result.append(";templateType=")
				.append(getTemplateType());
		}

		if (StringUtils.isNotBlank(getLocale())) {
			result.append(";locale=")
				.append(getLocale());
		}

		if (StringUtils.isNotBlank(getConfigName())) {
			result.append(";configName=")
				.append(getConfigName());
		}

		if (StringUtils.isNotBlank(getScope())) {
			result.append(";scope=")
				.append(getScope());
		}

		if (!getParameters().isEmpty()) {
			result.append(";parameters=");

			StringBuilder parameters = new StringBuilder();
			getParameters().entrySet()
				.forEach(e -> {
					parameters.append(e.getKey())
						.append('=')
						.append(e.getValue())
						.append(',');
				});
			parameters.deleteCharAt(parameters.length() - 1);

			OSGiHeader.quote(result, parameters.toString());
		}

		return result.toString();
	}
}
