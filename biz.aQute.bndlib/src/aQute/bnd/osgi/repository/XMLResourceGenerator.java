package aQute.bnd.osgi.repository;

import static aQute.bnd.osgi.repository.XMLResourceConstants.ATTR_NAME;
import static aQute.bnd.osgi.repository.XMLResourceConstants.ATTR_NAMESPACE;
import static aQute.bnd.osgi.repository.XMLResourceConstants.ATTR_REFERRAL_DEPTH;
import static aQute.bnd.osgi.repository.XMLResourceConstants.ATTR_REFERRAL_URL;
import static aQute.bnd.osgi.repository.XMLResourceConstants.ATTR_REPOSITORY_INCREMENT;
import static aQute.bnd.osgi.repository.XMLResourceConstants.ATTR_REPOSITORY_NAME;
import static aQute.bnd.osgi.repository.XMLResourceConstants.ATTR_TYPE;
import static aQute.bnd.osgi.repository.XMLResourceConstants.ATTR_VALUE;
import static aQute.bnd.osgi.repository.XMLResourceConstants.NS_URI;
import static aQute.bnd.osgi.repository.XMLResourceConstants.TAG_ATTRIBUTE;
import static aQute.bnd.osgi.repository.XMLResourceConstants.TAG_CAPABILITY;
import static aQute.bnd.osgi.repository.XMLResourceConstants.TAG_DIRECTIVE;
import static aQute.bnd.osgi.repository.XMLResourceConstants.TAG_REFERRAL;
import static aQute.bnd.osgi.repository.XMLResourceConstants.TAG_REPOSITORY;
import static aQute.bnd.osgi.repository.XMLResourceConstants.TAG_REQUIREMENT;
import static aQute.bnd.osgi.repository.XMLResourceConstants.TAG_RESOURCE;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.TypedAttribute;
import aQute.bnd.stream.MapStream;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

/**
 * Can turn an OSGi repository into an
 * {@code http://www.osgi.org/xmlns/repository/v1.0.0} XML file. See the
 * Repository spec in OSGi.
 */
public class XMLResourceGenerator {

	private Tag				repository	= new Tag(TAG_REPOSITORY);
	private Set<Resource>	visited		= new HashSet<>();
	private int				indent		= 0;
	private boolean			compress	= false;
	private URI				base;

	public XMLResourceGenerator() {
		repository.addAttribute("xmlns", NS_URI);
	}

	public void save(File location) throws IOException {
		if (location.getName()
			.endsWith(".gz"))
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

	/**
	 * Note that calling {@link #name(String)} sets increment to
	 * {@link System#currentTimeMillis()}. In order to retain backward
	 * compatibility that is not change. Therefore, in order to specify a value
	 * {@link #increment(long)} should be called after.
	 *
	 * @param name
	 * @return this
	 */
	public XMLResourceGenerator name(String name) {
		repository.addAttribute(ATTR_REPOSITORY_NAME, name);
		repository.addAttribute(ATTR_REPOSITORY_INCREMENT, System.currentTimeMillis());
		return this;
	}

	/**
	 * Note that calling {@link #name(String)} sets increment to
	 * {@link System#currentTimeMillis()}. In order to retain backward
	 * compatibility that is not change. Therefore, in order to specify a value
	 * {@link #increment(long)} should be called after.
	 *
	 * @param increment
	 * @return this
	 */
	public XMLResourceGenerator increment(long increment) {
		repository.addAttribute(ATTR_REPOSITORY_INCREMENT, increment);
		return this;
	}

	public XMLResourceGenerator referral(URI reference, int depth) {
		Tag referall = new Tag(repository, TAG_REFERRAL);
		referall.addAttribute(ATTR_REFERRAL_URL, reference);
		if (depth > 0)
			referall.addAttribute(ATTR_REFERRAL_DEPTH, depth);
		return this;
	}

	public XMLResourceGenerator repository(Repository repository) {
		Requirement wildcard = ResourceUtils.createWildcardRequirement();
		Map<Requirement, Collection<Capability>> findProviders = repository
			.findProviders(Collections.singleton(wildcard));
		findProviders.get(wildcard)
			.stream()
			.map(Capability::getResource)
			.forEach(this::resource);
		return this;
	}

	public XMLResourceGenerator resources(Collection<? extends Resource> resources) {
		resources.forEach(this::resource);
		return this;
	}

	public XMLResourceGenerator resource(Resource resource) {
		if (!visited.contains(resource)) {
			visited.add(resource);

			Tag r = new Tag(repository, TAG_RESOURCE);
			List<Capability> caps = resource.getCapabilities(null);
			caps.forEach(cap -> {
				Tag cr = new Tag(r, TAG_CAPABILITY);
				cr.addAttribute(ATTR_NAMESPACE, cap.getNamespace());
				directives(cr, cap.getDirectives());
				attributes(cr, cap.getAttributes());
			});

			List<Requirement> reqs = resource.getRequirements(null);
			reqs.forEach(req -> {
				Tag cr = new Tag(r, TAG_REQUIREMENT);
				cr.addAttribute(ATTR_NAMESPACE, req.getNamespace());
				directives(cr, req.getDirectives());
				attributes(cr, req.getAttributes());
			});
		}
		return this;
	}

	private void directives(Tag cr, Map<String, String> directives) {
		MapStream.of(directives)
			.forEach((key, value) -> {
				Tag d = new Tag(cr, TAG_DIRECTIVE);
				d.addAttribute(ATTR_NAME, key);
				d.addAttribute(ATTR_VALUE, value);
			});
	}

	private void attributes(Tag cr, Map<String, Object> attributes) {
		boolean isContent = isContent(cr);
		MapStream.of(attributes)
			.filterValue(Objects::nonNull)
			.mapValue(TypedAttribute::getTypedAttribute)
			.filterValue(Objects::nonNull)
			.forEach((key, ta) -> {
				String value = (isContent && ContentNamespace.CAPABILITY_URL_ATTRIBUTE.equals(key))
					? relativize(ta.value)
					: ta.value;

				Tag d = new Tag(cr, TAG_ATTRIBUTE);
				d.addAttribute(ATTR_NAME, key);
				d.addAttribute(ATTR_VALUE, value);
				if (ta.type != null)
					d.addAttribute(ATTR_TYPE, ta.type);
			});
	}

	private boolean isContent(Tag cr) {
		return ContentNamespace.CONTENT_NAMESPACE.equals(cr.getAttribute(ATTR_NAMESPACE));
	}

	private String relativize(String value) {
		if (base == null) {
			return value;
		}
		try {
			URI uri = new URI(value);
			return base.relativize(uri)
				.toString();
		} catch (URISyntaxException e) {
			return value;
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

	/**
	 * @param base the base URI from which the index urls are relative
	 */
	public XMLResourceGenerator base(URI base) {
		this.base = base;
		return this;
	}
}
