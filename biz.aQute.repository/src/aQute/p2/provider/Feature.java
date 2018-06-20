package aQute.p2.provider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

public class Feature extends XML {

	public static class Plugin {
		public String	id;
		public Version	version;

		@Override
		public String toString() {
			return id + ":" + version;
		}
	}

	List<Plugin> plugins = new ArrayList<>();

	public Feature(Document document) {
		super(document);
	}

	public Feature(InputStream in) throws Exception {
		this(toDoc(in));
	}

	private static Document toDoc(InputStream in) throws Exception {
		try (Jar jar = new Jar("feature", in)) {
			Resource resource = jar.getResource("feature.xml");
			if (resource == null) {
				throw new IllegalArgumentException("JAR does not contain proper 'feature.xml");
			}
			DocumentBuilder db = XML.dbf.newDocumentBuilder();
			Document doc = db.parse(resource.openInputStream());
			return doc;
		}
	}

	List<Plugin> getPlugins() throws Exception {
		NodeList nodes = getNodes("/feature/plugin");
		List<Plugin> result = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node item = nodes.item(i);
			Plugin plugin = getFromType(item, Plugin.class);
			result.add(plugin);
		}

		return result;
	}

	@Override
	public String toString() {
		return "Feature [plugins=" + plugins + "]";
	}
}
