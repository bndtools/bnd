package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.artifact.dto.MavenCoordinatesDTO;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;

/**
 * This plugin find and add the content of the pom.properties file to the
 * report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.MAVEN_COORDINATE)
public class MavenCoordinatePlugin implements ReportEntryPlugin<Jar>, Plugin {

	private Reporter					_reporter;
	private final Map<String, String>	_properties	= new HashMap<>();

	public MavenCoordinatePlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.MAVEN_COORDINATE);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Jar.class.getCanonicalName());
	}

	@Override
	public Object extract(final Jar jar, final Locale locale) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		MavenCoordinatesDTO dto = new MavenCoordinatesDTO();

		jar.getResources()
			.entrySet()
			.stream()
			.filter(e -> e.getKey()
				.endsWith("pom.properties"))
			.findAny()
			.ifPresent(e -> {
				final Properties p = new Properties();
				try {
					p.load(e.getValue()
						.openInputStream());
					dto.groupId = p.getProperty("groupId");
					dto.artifactId = p.getProperty("artifactId");
					dto.version = p.getProperty("version");
					// dto.type = p.getProperty("type");//may interrest on .eas
					dto.classifier = p.getProperty("classifier");

				} catch (final Exception exception) {
					_reporter.exception(exception, "Failed to read pom.properties file at %s", e.getKey());
				}
			});

		if (dto.groupId == null && dto.artifactId == null) {
			return null;
		}
		return dto;
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public void setReporter(final Reporter processor) {
		_reporter = processor;
	}
}
