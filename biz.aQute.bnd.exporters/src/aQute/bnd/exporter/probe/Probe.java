package aQute.bnd.exporter.probe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.dto.CapabilityDTO;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.Parameters;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

/**
 * Collects all capabilities of this environment and stores them in a file in
 * the home directory.
 *
 */
public class Probe extends Thread implements BundleActivator {
	static final String PROBE_QUERY = "probe.query";

	public static class ExporterProfile extends DTO {
		public String name;
		public long time = System.currentTimeMillis();
		public List<CapabilityDTO> capabilities = new ArrayList<CapabilityDTO>();
	}

	private ExporterProfile profile = new ExporterProfile();
	private BundleContext context;

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		start();
	}

	public void run() {
		try {
			String filename = context.getProperty(PROBE_QUERY);

			if (filename == null) {
				filename = "~/bnd.profile";
			}

			Scanner scanner = new Scanner(System.in);
			String line = scanner.nextLine().trim();

			if (line == null || line.trim().isEmpty())
				return;

			File file = IO.getFile(line.trim());

			System.out.println("Reading "+file.getAbsolutePath());
			
			profile.name = file.getName();

			doPackages(context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES));
			doPackages(context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA));
			doCapabilities(context.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES));
			doCapabilities(context.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA));

			for (Bundle b : context.getBundles()) {
				doPackages(b.getHeaders().get(Constants.EXPORT_PACKAGE));
				doCapabilities(b.getHeaders().get(Constants.PROVIDE_CAPABILITY));
			}

			new JSONCodec().enc().to(file).put(profile);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doCapabilities(String caps) {
		Parameters pa = new Parameters(caps);
		for (Entry<String, Attrs> e : pa.entrySet()) {
			CapabilityDTO cd = new CapabilityDTO();
			cd.namespace = removeDuplicateMarker(e.getKey());
			for (Entry<String, String> ad : e.getValue().entrySet()) {
				String key = ad.getKey();
				if (key.endsWith(":")) {
					if (cd.directives == null)
						cd.directives = new HashMap<String, String>();
					cd.directives.put(key.substring(0, key.length() - 1), ad.getValue());
				} else {
					if (cd.attributes == null)
						cd.attributes = new HashMap<String, Object>();

					Type type = e.getValue().getType(key);
					if (type == Type.STRING)
						cd.attributes.put(key, ad.getValue());
					else
						cd.attributes.put(key + ":" + type.toString(), ad.getValue());
				}
			}
			profile.capabilities.add(cd);
		}
	}

	private String removeDuplicateMarker(String key) {
		while (key.endsWith("~"))
			key = key.substring(0, key.length() - 1);
		return key;
	}

	private void doPackages(String exports) {
		Parameters pa = new Parameters(exports);
		for (Entry<String, Attrs> e : pa.entrySet()) {
			CapabilityDTO cd = new CapabilityDTO();
			cd.namespace = PackageNamespace.PACKAGE_NAMESPACE;

			cd.attributes = new HashMap<String, Object>();
			String pname = removeDuplicateMarker(e.getKey());
			cd.attributes.put(PackageNamespace.PACKAGE_NAMESPACE, pname);

			String version = e.getValue().getVersion();
			boolean okversion = false;
			if (version != null) {
				cd.attributes.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE + ":Version", new Version(e.getKey()));
				okversion = true;
			}

			for (Entry<String, String> ad : e.getValue().entrySet()) {
				String key = ad.getKey();
				if (okversion && key.equals(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE))
					continue;

				if (key.endsWith(":")) {
					if (cd.directives == null)
						cd.directives = new HashMap<String, String>();
					cd.directives.put(key.substring(0, key.length() - 1), ad.getValue());
				} else {
					Type type = e.getValue().getType(key);
					if (type == Type.STRING)
						cd.attributes.put(key, ad.getValue());
					else
						cd.attributes.put(key + ":" + type.toString(), ad.getValue());
				}
			}
			profile.capabilities.add(cd);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
