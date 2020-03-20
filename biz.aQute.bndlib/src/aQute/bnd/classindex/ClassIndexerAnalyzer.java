package aQute.bnd.classindex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.service.AnalyzerPlugin;

public class ClassIndexerAnalyzer implements AnalyzerPlugin {

	public static final String	X_CLASSINDEX	= "-x-classindex";
	public static final String	BND_HASHES		= "bnd.hashes";


	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		if (analyzer.getProperty(X_CLASSINDEX) == null)
			return false;

		for (Entry<PackageRef, Attrs> e : analyzer.getContained()
			.entrySet()) {
			PackageRef export = e.getKey();
			Attrs attrs = e.getValue();

			Set<Clazz> classspace = analyzer.getClassspace(export);
			if (classspace.isEmpty())
				continue;

			List<Integer> hashes = new ArrayList<>();
			for (Clazz c : classspace) {
				TypeRef tr = c.getClassName();
				if (tr.isArray() || tr.isNested())
					continue;

				int hash = tr.getShorterName()
					.hashCode();
				hashes.add(hash);
			}
			attrs.putTyped(BND_HASHES, hashes);
		}
		return false;
	}

	public static int hash(String last) {
		return last.hashCode();
	}

	public static boolean isEqual(String a, String b) {
		return a.equals(b);
	}
}
