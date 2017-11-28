package biz.aQute.bnd.reporter.plugins;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import aQute.bnd.service.reporter.ReportImportDeserializerPlugin;

public class PropertiesImportDeserializerPlugin implements ReportImportDeserializerPlugin {

	static private final String[] _ext = { "properties" };

	@Override
	public String[] getHandledExtensions() {
		return _ext;
	}

	@Override
	public Object deserialyze(final InputStream input) throws Exception {
		Objects.requireNonNull(input, "input");

		final Map<String, String> propDto = new LinkedHashMap<>();
		final Properties properties = new Properties();
		properties.load(input);

		for (final String key : properties.stringPropertyNames()) {
			propDto.put(key, properties.getProperty(key));
		}

		if (!propDto.isEmpty()) {
			return propDto;
		} else {
			return null;
		}
	}
}
