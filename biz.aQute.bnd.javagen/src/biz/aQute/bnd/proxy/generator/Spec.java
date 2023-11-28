package biz.aQute.bnd.proxy.generator;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;

/**
 * Analyzes the command line and turns it in a basic set of parameters
 */
class Spec {

	final Analyzer		analyzer;
	final PackageRef	pack;
	final TypeRef		base;
	final TypeRef		facade;
	final TypeRef[]		domains;

	public Spec(Analyzer analyzer, String spec, String pack, String base) {
		this.pack = analyzer.getPackageRef(pack);
		this.base = analyzer.getTypeRefFromFQN(base);
		this.analyzer = analyzer;
		String parts[] = spec.split(":");
		if (parts.length == 1) {
			TypeRef domain = analyzer.getTypeRefFromFQN(parts[0]);
			this.domains = new TypeRef[] {
				domain
			};
			this.facade = toTypeRef(analyzer, this.pack, domain.getShortName() + "Facade");
		} else {
			this.facade = toTypeRef(analyzer, this.pack, parts[0]);
			this.domains = new TypeRef[parts.length - 1];
			for (int i = 1; i < parts.length; i++) {
				this.domains[i - 1] = analyzer.getTypeRefFromFQN(parts[i]);
			}
		}
	}

	public String source() {
		try {
			Source f = new Source(analyzer, facade, base, domains);
			return f.source();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private static TypeRef toTypeRef(Analyzer analyzer, PackageRef pack, String fqnOrShort) {
		String name;
		if (fqnOrShort.contains("."))
			name = fqnOrShort;
		else
			name = pack.getFQN() + "." + fqnOrShort;

		return analyzer.getTypeRefFromFQN(name);
	}
}
