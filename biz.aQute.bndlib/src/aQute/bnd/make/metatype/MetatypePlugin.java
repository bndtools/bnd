package aQute.bnd.make.metatype;

import java.util.Collection;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.AnalyzerPlugin;

/**
 * This class is responsible for meta type types. It is a plugin that can
 * 
 * @author aqute
 */
public class MetatypePlugin implements AnalyzerPlugin {

	public boolean analyzeJar(Analyzer analyzer) throws Exception {

		Parameters map = analyzer.parseHeader(analyzer.getProperty(Constants.METATYPE));

		Jar jar = analyzer.getJar();
		for (String name : map.keySet()) {
			Collection<Clazz> metatypes = analyzer.getClasses("", QUERY.ANNOTATED.toString(),
					"aQute.bnd.annotation.metatype.Meta$OCD", //
					QUERY.NAMED.toString(), name //
			);
			for (Clazz c : metatypes) {
				analyzer.warning(
						"%s annotation used in class %s. Bnd metatype annotations are deprecated as of Bnd 3.2 and support will be removed in Bnd 4.0. Please change to use OSGi Metatype annotations.",
						"aQute.bnd.annotation.metatype.Meta$OCD", c);
				jar.putResource("OSGI-INF/metatype/" + c.getFQN() + ".xml", new MetaTypeReader(c, analyzer));
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "MetatypePlugin";
	}

}
