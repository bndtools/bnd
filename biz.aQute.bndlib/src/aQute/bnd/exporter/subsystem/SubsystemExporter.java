package aQute.bnd.exporter.subsystem;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.export.*;

@BndPlugin(name = "subsystem")
public class SubsystemExporter implements Exporter {

	@Override
	public String[] getTypes() {
		return null;
	}

	@Override
	public Resource export(String type, final Project project, Map<String,String> options) throws Exception {
		Jar jar = new Jar(".");

		Manifest a = new Manifest();
		Attributes application = a.getMainAttributes();

		for (String key : project.getPropertyKeys(true)) {
			String value = project.getProperty(key);
			if (value == null)
				continue;

			value = value.trim();
			if (value.isEmpty() || Constants.EMPTY_HEADER.equals(value))
				continue;

			char c = value.charAt(0);
			if (Character.isUpperCase(c))
				application.putValue(key, value);
		}

		set(application, "Subsystem-Type", "osgi.subsystem.application");
		set(application, "Subsystem-SymbolicName", project.getBsns().iterator().next());
		Manifest d = new Manifest();
		Attributes deployment = a.getMainAttributes();

		List<File> files = new ArrayList<File>();

		for (Container c : project.getRunbundles()) {
			c.contributeFiles(files, project);
		}

		for (File file : files) {
			Domain domain = Domain.domain(file);

		}

		jar.putResource("OSGI-INF/DEPLOYMENT.MF", new ManifestResource(d));
		jar.putResource("OSGI-INF/SUBSYSTEM.MF", new ManifestResource(a));
		
		return new JarResource(jar);
	}

	private void set(Attributes application, String string, String string2) {
		// TODO Auto-generated method stub

	}

}
