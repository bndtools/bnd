package aQute.bnd.apidiff;

import java.util.*;

import aQute.bnd.service.apidiff.*;
import aQute.lib.osgi.*;
import aQute.libg.version.*;

public class BundleDef extends Def<BundleDef> {
	final Analyzer					analyzer			= new Analyzer();
	final Map<String, PackageDef>	exportedPackages	= new HashMap<String, BundleDef.PackageDef>();

	class PackageDef extends Def<PackageDef> {
		final Map<String, ClassDef>	accessibleClasses	= new HashMap<String, ClassDef>();

		public PackageDef(String packageName, String version) {
			super(Type.PACKAGE, packageName, Version.parseVersion(version));
		}

		public Delta getDelta(PackageDef older) {
			return null;
		}

		public Map<String, ? extends Def> getChildren() {
			return accessibleClasses;
		}

	}

	public BundleDef(Jar jar) throws Exception {
		super(Type.BUNDLE, jar.getBsn(), Version.parseVersion(jar.getVersion()));

		this.analyzer.setJar(jar);
		this.analyzer.analyze();

		// TODO exclude and include classes
		
		Map<String, Map<String, String>> exports = analyzer.getExports();
		for (Map.Entry<String, Map<String, String>> entry : exports.entrySet()) {
			String packageName = Processor.removeDuplicateMarker(entry.getKey());
			PackageDef pd = exportedPackages.get(packageName);
			if (pd == null) {
				pd = new PackageDef(packageName, entry.getValue().get(Constants.VERSION_ATTRIBUTE));
				exportedPackages.put(packageName, pd);
			} else {
				// TODO handle multiple exports
			}
		}

		// Optimize the classes because we must match the class space to the
		// packages and do not want to do this iteration for every
		// package access

		for (Clazz c : analyzer.getClassspace().values()) {
			if (c.isPublic() || c.isProtected()) {
				String packageName = c.getPackage();
				PackageDef pd = exportedPackages.get(packageName);
				if (pd != null) {
					pd.accessibleClasses.put(c.getFQN(), new ClassDef(c));
				}
			}
		}
	}

	public Map<String, ? extends Def> getChildren() {
		return exportedPackages;
	}

}
