package aQute.lib.spring;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Descriptors.PackageRef;

public class XMLType {

	Transformer		transformer;
	Pattern			paths;
	String			root;

	static Pattern	QN	= Pattern.compile("[_A-Za-z$][_A-Za-z0-9$]*(\\.[_A-Za-z$][_A-Za-z0-9$]*)*");

	public XMLType(URL source, String root, String paths) throws Exception {
		transformer = getTransformer(source);
		this.paths = Pattern.compile(paths);
		this.root = root;
	}

	public Set<String> analyze(InputStream in) throws Exception {
		Set<String> refers = new HashSet<String>();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Result r = new StreamResult(bout);
		Source s = new StreamSource(in);
		transformer.transform(s, r);

		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		bout.close();

		BufferedReader br = new BufferedReader(new InputStreamReader(bin, "UTF8"));

		String line = br.readLine();
		while (line != null) {
			line = line.trim();
			if (line.length() > 0) {
				String parts[] = line.split("\\s*,\\s*");
				for (int i = 0; i < parts.length; i++) {
					int n = parts[i].lastIndexOf('.');
					if (n > 0) {
						refers.add(parts[i].subSequence(0, n).toString());
					}
				}
			}
			line = br.readLine();
		}
		br.close();
		return refers;
	}

	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Jar jar = analyzer.getJar();
		Map<String,Resource> dir = jar.getDirectories().get(root);
		if (dir == null || dir.isEmpty()) {
			Resource resource = jar.getResource(root);
			if (resource != null)
				process(analyzer, root, resource);
			return false;
		}

		for (Iterator<Map.Entry<String,Resource>> i = dir.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String,Resource> entry = i.next();
			String path = entry.getKey();
			Resource resource = entry.getValue();
			if (paths.matcher(path).matches()) {
				process(analyzer, path, resource);
			}
		}
		return false;
	}

	private void process(Analyzer analyzer, String path, Resource resource) {
		try {
			InputStream in = resource.openInputStream();
			Set<String> set = analyze(in);
			in.close();
			for (Iterator<String> r = set.iterator(); r.hasNext();) {
				PackageRef pack = analyzer.getPackageRef(r.next());
				if (!QN.matcher(pack.getFQN()).matches())
					analyzer.warning("Package does not seem a package in spring resource (" + path + "): " + pack);
				if (!analyzer.getReferred().containsKey(pack))
					analyzer.getReferred().put(pack);
			}
		}
		catch (Exception e) {
			analyzer.error("Unexpected exception in processing spring resources(" + path + "): " + e);
		}
	}

	protected Transformer getTransformer(java.net.URL url) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Source source = new StreamSource(url.openStream());
		return tf.newTransformer(source);
	}
}
