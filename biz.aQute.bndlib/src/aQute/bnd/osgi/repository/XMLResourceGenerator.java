package aQute.bnd.osgi.repository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.TypedAttribute;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

/**
 * Can turn an OSGi repository into an
 * {@code http://www.osgi.org/xmlns/repository/v1.0.0} XML file. See the
 * Repository spec in OSGi.
 */
public class XMLResourceGenerator {

	private Tag				repository	= new Tag("repository");
	private Set<Resource>	visited		= new HashSet<>();
	private int				indent		= 2;
	private boolean			compress	= false;

	public XMLResourceGenerator() {
		repository.addAttribute("xmlns", "http://www.osgi.org/xmlns/repository/v1.0.0");
	}

	public void save(File location) throws IOException {
		if (location.getName().endsWith(".gz"))
			compress = true;

		IO.mkdirs(location.getParentFile());
		File tmp = IO.createTempFile(location.getParentFile(), "index", ".xml");

		try (OutputStream out = IO.outputStream(tmp)) {
			save(out);
		}
		IO.rename(tmp, location);
	}

	public void save(OutputStream out) throws IOException {
		try {
			if (compress) {
				out = new GZIPOutputStream(out);
			}

			try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
					PrintWriter pw = new PrintWriter(writer)) {
					pw.printf("<?xml version='1.0' encoding='UTF-8'?>\n");
					repository.print(indent, pw);
			}
		} finally {
			out.close();
		}
	}

	public XMLResourceGenerator name(String name) {
		repository.addAttribute("name", name);
		repository.addAttribute("increment", System.currentTimeMillis());
		return this;
	}

	public XMLResourceGenerator referral(URI reference, int depth) {
		Tag referall = new Tag(repository, "referral");
		referall.addAttribute("url", reference);
		if (depth > 0)
			referall.addAttribute("depth", depth);
		return this;
	}

	public XMLResourceGenerator repository(Repository repository) throws Exception {
		Requirement wildcard = ResourceUtils.createWildcardRequirement();
		Map<Requirement,Collection<Capability>> findProviders = repository
				.findProviders(Collections.singleton(wildcard));
		for (Capability capability : findProviders.get(wildcard)) {
			resource(capability.getResource());
		}
		return this;
	}

	public XMLResourceGenerator resources(Collection< ? extends Resource> resources) throws Exception {
		for (Resource resource : resources) {
			resource(resource);
		}
		return this;
	}

	public XMLResourceGenerator resource(Resource resource) throws Exception {
		if (!visited.contains(resource)) {
			visited.add(resource);

			Tag r = new Tag(repository, "resource");
			for (Capability cap : resource.getCapabilities(null)) {
				Tag cr = new Tag(r, "capability");
				cr.addAttribute("namespace", cap.getNamespace());
				directives(cr, cap.getDirectives());
				attributes(cr, cap.getAttributes());
			}

			for (Requirement req : resource.getRequirements(null)) {
				Tag cr = new Tag(r, "requirement");
				cr.addAttribute("namespace", req.getNamespace());
				directives(cr, req.getDirectives());
				attributes(cr, req.getAttributes());
			}
		}
		return this;
	}

	private void directives(Tag cr, Map<String,String> directives) {
		for (Entry<String,String> e : directives.entrySet()) {
			Tag d = new Tag(cr, "directive");
			String key = e.getKey();
			if (key.endsWith(":")) {
				key = key.substring(0, key.length() - 1);
			}
			d.addAttribute("name", key);
			d.addAttribute("value", e.getValue());
		}
	}

	private void attributes(Tag cr, Map<String,Object> atrributes) throws Exception {
		for (Entry<String,Object> e : atrributes.entrySet()) {
			Object value = e.getValue();
			if (value == null)
				continue;

			TypedAttribute ta = TypedAttribute.getTypedAttribute(value);
			if (ta == null)
				continue;

			Tag d = new Tag(cr, "attribute");
			d.addAttribute("name", e.getKey());
			d.addAttribute("value", ta.value);
			if (ta.type != null)
				d.addAttribute("type", ta.type);

		}
	}

	public XMLResourceGenerator indent(int n) {
		this.indent = n;
		return this;
	}

	public XMLResourceGenerator compress() {
		this.compress = true;
		return this;
	}
}
