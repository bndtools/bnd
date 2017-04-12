package aQute.bnd.deployer.repository.providers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;

import aQute.bnd.service.Plugin;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.service.reporter.Reporter;

public class KnownBundleAnalyzerPlugin extends KnownBundleAnalyzer implements ResourceAnalyzer, Plugin {

	private static final String	PROP_DATA	= "data";

	Reporter					reporter;

	public KnownBundleAnalyzerPlugin() {
		super(new UTF8Properties());
	}

	public void setProperties(Map<String,String> config) {
		String fileName = config.get(PROP_DATA);
		if (fileName == null)
			throw new IllegalArgumentException(
					String.format("Property name '%s' must be set on KnownBundleAnalyzerPlugin", PROP_DATA));
		File file = new File(fileName);
		if (!file.isFile())
			throw new IllegalArgumentException(
					String.format("Data file does not exist, or is not a plain file: %s", file));

		try (InputStream stream = IO.stream(file)) {
			Properties props = new UTF8Properties();
			props.load(stream);
			setKnownBundlesExtra(props);
		} catch (IOException e) {
			throw new IllegalArgumentException(String.format("Unable to read data file: %s", file), e);
		}
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

}
