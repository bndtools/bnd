package aQute.bnd.resolver;

import java.util.*;
import java.util.jar.*;

import aQute.lib.osgi.*;
import aQute.libg.version.*;

public class Resource {
	enum Type {
		PACKAGE,
		BUNDLE,
		HOST;
	}
	
	final Set<Requirement>	requirements	= new HashSet<Requirement>();
	final Set<Capability>	capabilities	= new HashSet<Capability>();
	final List<Resource>	fragments;
	final String			bsn;
	final Version			version;
	final boolean			singleton;
	final Resolver			resolver;

	class Requirement {
		final Type		type;
		final String		name;
		final VersionRange	range;
		final boolean		optional;

		Requirement(Type type, String name, VersionRange range, boolean optional) {
			this.type = type;
			this.name = name;
			this.range = range;
			this.optional = optional;
		}

		Resource getDeclaredResource() {
			return Resource.this;
		}

		public boolean matches(Capability cap) {
			boolean a = cap.type == type;
			boolean b = cap.name.equals(name);
			boolean c = range.includes(cap.version);
			return a && b && c;
		}
		
		public String toString() {
			return "R." +type + ":" + name + "-" + range;
		}

	}

	class Capability {
		final Type	type;
		final String	name;
		final Version	version;

		Capability(Type type, String name, Version version) {
			this.type = type;
			this.name = name;
			this.version = version;
		}

		Resource getDeclaredResource() {
			return Resource.this;
		}
		public String toString() {
			return "C." + type + ":" + name + "-" + version;
		}
	}

	Resource(Resolver resolver, Manifest m) {
		this.resolver= resolver;
		Attributes main = m.getMainAttributes();
		Map<String, Map<String, String>> bsns = resolver.parseHeader(main
				.getValue(Constants.BUNDLE_SYMBOLICNAME));
		if (bsns.size() > 1)
			resolver.error("Multiple bsns %s", bsns);
		if (bsns.size() < 1) {
			resolver.error("No bsns");
			bsn = "<not set>";
			version = new Version("0");
			singleton = false;
		} else {
			Map.Entry<String, Map<String, String>> entry = bsns.entrySet().iterator().next();
			bsn = entry.getKey();
			String v = main.getValue(Constants.BUNDLE_VERSION);
			this.version = version(v);
			singleton = "true"
					.equalsIgnoreCase(entry.getValue().get(Constants.SINGLETON_DIRECTIVE));

			String directive = entry.getValue().get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
			boolean attach = directive == null || "always".equals(directive) || "resolve-time".equals(directive);
			if (attach) {
				capabilities.add(new Capability(Type.HOST, bsn, version));
			}
		}
		Map<String, Map<String, String>> hosts = resolver.parseHeader(main.getValue(Constants.FRAGMENT_HOST));
		if (hosts.size() > 1)
			resolver.error("Multiple fragment hosts %s", hosts);
		if (hosts.size() == 1) {
			Map.Entry<String, Map<String, String>> entry = hosts.entrySet().iterator().next();
			String host = entry.getKey();
			VersionRange range = range(entry.getValue().get(Constants.VERSION_ATTRIBUTE));
			requirements.add(new Requirement(Type.HOST, host, range, false));
			fragments = null;
		} else {
			fragments = new ArrayList<Resource>();
			capabilities.add(new Capability(Type.BUNDLE, bsn, version));
		}

		Map<String, Map<String, String>> rbs = resolver.parseHeader(main.getValue(Constants.REQUIRE_BUNDLE));
		for (Map.Entry<String, Map<String, String>> clause : rbs.entrySet()) {
			boolean optional = "optional".equals(clause.getValue().get(
					Constants.RESOLUTION_DIRECTIVE));
			requirements.add(new Requirement(Type.BUNDLE, clause.getKey(), range(clause
					.getValue().get(Constants.VERSION_ATTRIBUTE)), optional));
		}
		Map<String, Map<String, String>> imports = resolver.parseHeader(main
				.getValue(Constants.IMPORT_PACKAGE));
		for (Map.Entry<String, Map<String, String>> clause : imports.entrySet()) {
			boolean optional = "optional".equals(clause.getValue().get(
					Constants.RESOLUTION_DIRECTIVE));
			requirements.add(new Requirement(Type.PACKAGE, clause.getKey(), range(clause
					.getValue().get(Constants.VERSION_ATTRIBUTE)), optional));
		}
		Map<String, Map<String, String>> exports = resolver.parseHeader(main
				.getValue(Constants.EXPORT_PACKAGE));
		for (Map.Entry<String, Map<String, String>> clause : exports.entrySet()) {
			capabilities.add(new Capability(Type.PACKAGE, clause.getKey(), version(clause
					.getValue().get(Constants.VERSION_ATTRIBUTE))));
		}
	}

	private VersionRange range(String string) {
		try {
			return new VersionRange(string);
		} catch (NullPointerException e) {
			return new VersionRange("0");
		} catch (Exception e) {
			resolver.error("Invalid version range: %s in %s-%s", string, bsn, version);
			return new VersionRange("0");
		}
	}

	private Version version(String string) {
		try {
			return new Version(string);
		} catch (NullPointerException e) {
			return new Version("0");
		} catch (Exception e) {
			resolver.error("Invalid version: %s in %s-%s", string, bsn, version);
			return new Version("0");
		}
	}
	
	public String toString() {
		return bsn + "-" + version;
	}
}
