package biz.aQute.bnd.reporter.generator;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.osgi.Jar;

public class ReportConfig {

	private String _inputPath = null;
	private Jar _inputJar = null;
	private String _locale = null;
	private List<String> _templatePaths = new LinkedList<>();
	private List<String> _includePaths = new LinkedList<>();
	private Map<String, String> _includeTypes = new HashMap<>();
	private Map<String, String> _includeParents = new HashMap<>();
	private String _outputPath = null;
	private OutputStream _outputStream = null;

	private ReportConfig() {
	}

	public List<String> getTemplatePaths() {
		return _templatePaths;
	}

	public List<String> getIncludePaths() {
		return _includePaths;
	}

	public String getIncludeType(final String includePath) {
		return _includeTypes.get(includePath);
	}

	public String getIncludeParent(final String includePath) {
		return _includeParents.get(includePath);
	}

	public String getInputPath() {
		return _inputPath;
	}

	public String getOutputPath() {
		return _outputPath;
	}

	public OutputStream getOutputStream() {
		return _outputStream;
	}

	public Jar getInputJar() {
		return _inputJar;
	}

	public String getLocale() {
		return _locale;
	}

	public static Builder builder(final Jar inputJar) {
		Objects.requireNonNull(inputJar, "inputJar");

		return new Builder(inputJar);
	}

	public static Builder builder(final String inputPath) {
		Objects.requireNonNull(inputPath, "inputPath");

		return new Builder(inputPath);
	}

	public static class Builder {

		private final ReportConfig _config;

		private Builder() {
			_config = new ReportConfig();
			_config._locale = "";
			_config._templatePaths = new LinkedList<>();
			_config._includePaths = new LinkedList<>();
			_config._includeTypes = new HashMap<>();
			_config._includeParents = new HashMap<>();
			_config._outputStream = System.out;
		}

		public Builder(final Jar inputJar) {
			this();

			_config._inputJar = inputJar;
		}

		public Builder(final String inputPath) {
			this();

			_config._inputPath = inputPath;
		}

		public Builder addTemplates(final String templatePath) {
			Objects.requireNonNull(templatePath, "templatePath");

			_config._templatePaths.add(templatePath);

			return this;
		}

		public Builder addIncludePath(final String includePath, final String type, final String parentName) {
			Objects.requireNonNull(includePath, "includePath");

			_config._includePaths.add(includePath);

			if (type != null && !type.isEmpty()) {
				_config._includeTypes.put(includePath, type);
			}

			if (parentName != null && !parentName.isEmpty()) {
				_config._includeParents.put(includePath, parentName);
			}

			return this;
		}

		public Builder setLocale(final String locale) {
			Objects.requireNonNull(locale, "locale");

			_config._locale = locale;

			return this;
		}

		public Builder setOutput(final String outputPath) {
			Objects.requireNonNull(outputPath, "outputPath");

			_config._outputPath = outputPath;

			return this;
		}

		public Builder setOutput(final OutputStream outputStream) {
			Objects.requireNonNull(outputStream, "outputStream");

			_config._outputStream = outputStream;

			return this;
		}

		public ReportConfig build() {

			final ReportConfig config = new ReportConfig();

			config._inputJar = _config._inputJar;
			config._inputPath = _config._inputPath;
			config._locale = _config._locale;
			config._templatePaths = new LinkedList<>(_config._templatePaths);
			config._includePaths = new LinkedList<>(_config._includePaths);
			config._includeParents = new HashMap<>(_config._includeParents);
			config._includeTypes = new HashMap<>(_config._includeTypes);
			config._outputPath = _config._outputPath;
			config._outputStream = _config._outputStream;

			return config;
		}
	}
}
