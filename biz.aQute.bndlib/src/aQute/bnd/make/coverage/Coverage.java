package aQute.bnd.make.coverage;

import java.io.*;
import java.util.*;

import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.TypeRef;

/**
 * This class can create a coverage table between two classspaces. The
 * destination class space is used to create a table of methods. All source
 * methods that refer to a specific dest are then filled into the table.
 */
public class Coverage {

	/**
	 * Create a cross reference table from source to dest.
	 * 
	 * @param source
	 *            The methods that refer to dest
	 * @param dest
	 *            The methods that are being referred to
	 * @return A mapping of source methods to destination methods.
	 * @throws IOException
	 */
	public static Map<MethodDef,List<MethodDef>> getCrossRef(Collection<Clazz> source, Collection<Clazz> dest)
			throws Exception {
		final Map<MethodDef,List<MethodDef>> catalog = buildCatalog(dest);
		crossRef(source, catalog);
		return catalog;
	}

	private static void crossRef(Collection<Clazz> source, final Map<MethodDef,List<MethodDef>> catalog)
			throws Exception {
		for (final Clazz clazz : source) {
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef	source;

				public void implementsInterfaces(TypeRef names[]) {
					MethodDef def = clazz.getMethodDef(0, "<implements>", "()V");
					// TODO
					for (TypeRef interfaceName : names) {
						for (Map.Entry<MethodDef,List<MethodDef>> entry : catalog.entrySet()) {
							String catalogClass = entry.getKey().getContainingClass().getFQN();
							List<MethodDef> references = entry.getValue();

							if (catalogClass.equals(interfaceName.getFQN())) {
								references.add(def);
							}
						}
					}
				}

				// Method definitions
				public void method(MethodDef source) {
					this.source = source;
				}

				public void reference(MethodDef reference) {
					List<MethodDef> references = catalog.get(reference);
					if (references != null) {
						references.add(source);
					}
				}
			});
		}
	}

	private static Map<MethodDef,List<MethodDef>> buildCatalog(Collection<Clazz> sources) throws Exception {
		final Map<MethodDef,List<MethodDef>> catalog = new TreeMap<MethodDef,List<MethodDef>>(
				new Comparator<MethodDef>() {
					public int compare(MethodDef a, MethodDef b) {
						return a.getName().compareTo(b.getName());
					}
				});
		for (final Clazz clazz : sources) {
			clazz.parseClassFileWithCollector(new ClassDataCollector() {

				public boolean classStart(int access, TypeRef name) {
					return clazz.isPublic();
				}

				public void method(MethodDef source) {
					if (source.isPublic() || source.isProtected())
						catalog.put(source, new ArrayList<MethodDef>());
				}

			});
		}
		return catalog;
	}

}
