package aQute.bnd.make.metatype;

import java.util.Collection;

import aQute.bnd.annotation.metatype.Meta;
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
			Collection<Clazz> metatypes = analyzer.getClasses("", QUERY.ANNOTATED.toString(), Meta.OCD.class.getName(), //
					QUERY.NAMED.toString(), name //
			);
			for (Clazz c : metatypes) {
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
