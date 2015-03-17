package aQute.bnd.exporter.subsystem;

import static aQute.bnd.osgi.Constants.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.export.*;

@BndPlugin(name = "subsystem")
public class SubsystemExporter implements Exporter {

	private static final String	OSGI_INF_SUBSYSTEM_MF		= "OSGI-INF/SUBSYSTEM.MF";
	private static final String	SUBSYSTEM_SYMBOLIC_NAME		= "Subsystem-SymbolicName";
	private static final String	OSGI_SUBSYSTEM_APPLICATION	= "osgi.subsystem.application";
	private static final String	SUBSYSTEM_TYPE				= "Subsystem-Type";
	private static final String	SUBSYSTEM_CONTENT			= "Subsystem-Content";

	@Override
	public String[] getTypes() {
		return new String[] {
			OSGI_SUBSYSTEM_APPLICATION
		};
	}

	@Override
	public Map.Entry<String,Resource> export(String type, final Project project, Map<String,String> options)
			throws Exception {
		Jar jar = new Jar(".");

		project.addClose(jar);

		Manifest a = new Manifest();
		Attributes application = a.getMainAttributes();

		List<File> files = new ArrayList<File>();

		for (Container c : project.getRunbundles()) {
			c.contributeFiles(files, project);
		}

		Parameters subsysContent = new Parameters();
		Instructions contentDecorators = new Instructions(project.getProperty(SUBSYSTEM_CONTENT, "*"));

		for (File file : files) {

			Domain domain = Domain.domain(file);
			String bsn = domain.getBundleSymbolicName().getKey();
			String version = domain.getBundleVersion();

			Instruction decorator = contentDecorators.finder(bsn);
			if (decorator == null || decorator.isNegated())
				continue;

			Attrs attrs = new Attrs(contentDecorators.get(decorator));
			attrs.put(Constants.VERSION_ATTRIBUTE, version);
			subsysContent.put(bsn, attrs);

			String path = bsn + "-" + version + ".jar";
			jar.putResource(path, new FileResource(file));
		}

		application.putValue(SUBSYSTEM_CONTENT, subsysContent.toString());

		headers(project, application);
		set(application, SUBSYSTEM_TYPE, type);

		String ssn = project.getName();

		Collection<String> bsns = project.getBsns();
		if (bsns.size() > 0) {
			ssn = bsns.iterator().next();
		}

		set(application, SUBSYSTEM_SYMBOLIC_NAME, ssn);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		a.write(bout);

		jar.putResource(OSGI_INF_SUBSYSTEM_MF, new EmbeddedResource(bout.toByteArray(), 0));

		final JarResource jarResource = new JarResource(jar);
		final String name = ssn + ".esa";

		return new Map.Entry<String,Resource>() {

			@Override
			public String getKey() {
				return name;
			}

			@Override
			public Resource getValue() {
				return jarResource;
			}

			@Override
			public Resource setValue(Resource arg0) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private void headers(final Project project, Attributes application) {
		for (String key : project.getPropertyKeys(true)) {

			if (application.getValue(key) != null)
				continue;

			String value = project.getProperty(key);
			if (value == null)
				continue;

			value = value.trim();
			if (value.isEmpty() || Constants.EMPTY_HEADER.equals(value))
				continue;

			char c = value.charAt(0);
			if (Character.isUpperCase(c) && Verifier.HEADER_PATTERN.matcher(key).matches())
				application.putValue(key, value);
		}
		Instructions instructions = new Instructions(project.mergeProperties(REMOVEHEADERS));
		Collection<Object> result = instructions.select(application.keySet(), false);
		application.keySet().removeAll(result);
	}

	private void set(Attributes application, String key, String... values) {
		if (application.getValue(key) != null)
			return;

		for (String value : values) {
			if (value != null) {
				application.putValue(key, value);
			}
		}

	}

}
