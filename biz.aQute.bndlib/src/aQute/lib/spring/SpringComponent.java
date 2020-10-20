package aQute.lib.spring;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.lib.io.IO;
import aQute.lib.xml.XML;

/**
 * This component is called when we find a resource in the META-INF/*.xml
 * pattern. We parse the resource and and the imports to the builder. Parsing is
 * done with XSLT (first time I see the use of having XML for the Spring
 * configuration files!).
 *
 * @author aqute
 */
@BndPlugin(name = "Spring")
public class SpringComponent implements AnalyzerPlugin {
	static Transformer				transformer;
	private final static Pattern	SPRING_SOURCE	= Pattern.compile("META-INF/spring/.*\\.xml");
	private final static Pattern	QN				= Pattern
		.compile("[_A-Za-z$][_A-Za-z0-9$]*(\\.[_A-Za-z$][_A-Za-z0-9$]*)*");

	public static Set<CharSequence> analyze(InputStream in) throws Exception {
		if (transformer == null) {
			TransformerFactory tf = XML.newTransformerFactory();
			Source source = new StreamSource(SpringComponent.class.getResourceAsStream("extract.xsl"));
			transformer = tf.newTransformer(source);
		}

		Set<CharSequence> refers = new HashSet<>();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Result r = new StreamResult(bout);
		Source s = new StreamSource(in);
		transformer.transform(s, r);

		try (BufferedReader br = IO.reader(bout.toString("UTF-8"))) {
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0) {
					String parts[] = line.split("\\s*,\\s*");
					for (String part : parts) {
						int n = part.lastIndexOf('.');
						if (n > 0) {
							refers.add(part.subSequence(0, n));
						}
					}
				}
				line = br.readLine();
			}
		}

		return refers;
	}

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Jar jar = analyzer.getJar();
		Map<String, Resource> dir = jar.getDirectory("META-INF/spring");
		if (dir == null || dir.isEmpty())
			return false;

		for (Entry<String, Resource> entry : dir.entrySet()) {
			String path = entry.getKey();
			Resource resource = entry.getValue();
			if (SPRING_SOURCE.matcher(path)
				.matches()) {
				try {
					Set<CharSequence> refers;
					try (InputStream in = resource.openInputStream()) {
						refers = analyze(in);
					}
					for (CharSequence refer : refers) {
						PackageRef pack = analyzer.getPackageRef(refer.toString());
						if (!QN.matcher(pack.getFQN())
							.matches())
							analyzer.warning("Package does not seem a package in spring resource (%s): %s", path, pack);
						if (!analyzer.getReferred()
							.containsKey(pack))
							analyzer.getReferred()
								.put(pack, new Attrs());
					}
				} catch (Exception e) {
					analyzer.error("Unexpected exception in processing spring resources(%s): %s", path, e);
				}
			}
		}
		return false;
	}

}
