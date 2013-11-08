package aQute.bnd.deployer.repository.providers;

import java.io.*;
import java.util.*;

import org.osgi.service.indexer.*;
import org.osgi.service.indexer.impl.*;

import aQute.bnd.service.*;
import aQute.service.reporter.*;

public class KnownBundleAnalyzerPlugin extends KnownBundleAnalyzer implements ResourceAnalyzer, Plugin {
	
	private static final String	PROP_DATA	= "data";

	Reporter	reporter;

	public KnownBundleAnalyzerPlugin() {
		super(new Properties());
	}

	public void setProperties(Map<String,String> config) {
		String fileName = config.get(PROP_DATA);
		if (fileName == null)
			throw new IllegalArgumentException(String.format("Property name '%s' must be set on KnownBundleAnalyzerPlugin", PROP_DATA));
		File file = new File(fileName);
		if (!file.isFile())
			throw new IllegalArgumentException(String.format("Data file does not exist, or is not a plain file: %s", file));

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file);
			Properties props = new Properties();
			props.load(stream);
			setKnownBundlesExtra(props);
		} catch (IOException e) {
			throw new IllegalArgumentException(String.format("Unable to read data file: %s", file), e);
		} finally {
			try { if (stream != null) stream.close(); } catch (IOException e) {}
		}
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

}
