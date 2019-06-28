package aQute.lib.spring;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

public class XMLType {

	Transformer						transformer;
	Pattern							paths;
	String							root;

	private final static Pattern	QN	= Pattern.compile("[_A-Za-z$][_A-Za-z0-9$]*(\\.[_A-Za-z$][_A-Za-z0-9$]*)*");

	public XMLType(URL source, String root, String paths) throws Exception {
		transformer = getTransformer(source);
		this.paths = Pattern.compile(paths);
		this.root = root;
	}

	public Set<String> analyze(InputStream in) throws Exception {
		Set<String> refers = new HashSet<>();

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
					for (int i = 0; i < parts.length; i++) {
						String pack = toPackage(parts[i]);
						if (pack != null)
							refers.add(pack);
					}
				}
				line = br.readLine();
			}
		}
		return refers;
	}

	static String toPackage(String fqn) {
		int n = fqn.lastIndexOf('.');
		if (n < 0 || n + 1 >= fqn.length())
			return null;

		char c = fqn.charAt(n + 1);
		if (Character.isJavaIdentifierStart(c) && Character.isUpperCase(c)) {
			String other = fqn.substring(0, n);
			return toPackage(other);
		}

		return fqn;
	}

	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Jar jar = analyzer.getJar();
		Map<String, Resource> dir = jar.getDirectory(root);
		if (dir == null || dir.isEmpty()) {
			Resource resource = jar.getResource(root);
			if (resource != null)
				process(analyzer, root, resource);
			return false;
		}

		for (Iterator<Map.Entry<String, Resource>> i = dir.entrySet()
			.iterator(); i.hasNext();) {
			Map.Entry<String, Resource> entry = i.next();
			String path = entry.getKey();
			Resource resource = entry.getValue();
			if (paths.matcher(path)
				.matches()) {
				process(analyzer, path, resource);
			}
		}
		return false;
	}

	private void process(Analyzer analyzer, String path, Resource resource) {
		try {
			Set<String> set;
			try (InputStream in = resource.openInputStream()) {
				set = analyze(in);
			}
			for (Iterator<String> r = set.iterator(); r.hasNext();) {
				PackageRef pack = analyzer.getPackageRef(r.next());
				if (!QN.matcher(pack.getFQN())
					.matches())
					analyzer.warning("Package does not seem a package in spring resource (%s): %s", path, pack);
				if (!analyzer.getReferred()
					.containsKey(pack))
					analyzer.getReferred()
						.put(pack);
			}
		} catch (Exception e) {
			analyzer.error("Unexpected exception in processing spring resources(%s): %s", path, e);
		}
	}

	protected Transformer getTransformer(java.net.URL url) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Source source = new StreamSource(url.openStream());
		return tf.newTransformer(source);
	}
}
