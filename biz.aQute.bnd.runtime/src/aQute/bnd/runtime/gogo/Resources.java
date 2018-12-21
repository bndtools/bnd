package aQute.bnd.runtime.gogo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import aQute.lib.dtoformatter.DTOFormatter;
import aQute.lib.io.IO;

public class Resources {

	private BundleContext	context;
	private int				prefix;

	public Resources(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		this.prefix = this.context.getBundle()
			.getEntry("META-INF/MANIFEST.MF")
			.getPath()
			.toString()
			.length() - 21;
	}

	@Descriptor("List the resource entries in a bundle")
	public List<String> entry(Bundle b) {
		Enumeration<URL> entries = b.findEntries("", "*", true);
		List<String> paths = new ArrayList<>();
		while (entries.hasMoreElements()) {

			String path = entries.nextElement()
				.getPath()
				.substring(prefix);
			if (!path.endsWith("/"))
				paths.add(path);
		}
		return paths;
	}

	@Descriptor("View a resource entry in a bundle")
	public String entry(Bundle b, String path) throws IOException {
		URL entry = b.getEntry(path);
		if (entry == null)
			return null;

		return IO.collect(entry.openStream());
	}
}
