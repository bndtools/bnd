package aQute.p2.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Manifest;

import org.osgi.framework.VersionRange;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.hex.Hex;
import aQute.lib.strings.Strings;

/**
 * Contains domain classes to hold a P2 repository
 */
class P2 {
	enum IUType {
		bundle,
		feature,
		other;
	}

	/**
	 * Base class for provides and requires
	 */
	static abstract class PR implements Comparable<PR> {
		final String	namespace;
		final BundleId	id;
		final IUType	type;

		PR(String namespace, BundleId id, IUType type) {
			this.namespace = namespace;
			this.id = id;
			this.type = type;
		}

		@Override
		public int compareTo(PR o) {
			int n = namespace.compareTo(o.namespace);
			if (n != 0)
				return n;

			return id.compareTo(o.id);
		}
	}

	static class Required extends PR {

		final VersionRange	range;
		final boolean		optional;

		Required(String namespace, BundleId id, IUType type, boolean optional) {
			super(namespace, id, type);
			this.optional = optional;
			range = new VersionRange(id.getVersion());
		}

		@Override
		public String toString() {
			return "Required [namespace=" + namespace + ", id=" + id + ", range=" + range + "]";
		}

	}

	static class Provided extends PR {

		Provided(String namespace, BundleId id, IUType type) {
			super(namespace, id, type);
		}

		@Override
		public String toString() {
			return "Provided [namespace=" + namespace + ", id=" + id + "]";
		}
	}

	/**
	 * Base class for IUs, the 'installable unit'.
	 */
	static abstract class IU implements Comparable<IU> {
		final BundleId				id;
		final Map<String, String>	properties	= new TreeMap<>();
		final Set<Required>			requires	= new TreeSet<>();
		final Set<Provided>			provides	= new TreeSet<>();
		final Domain				domain;

		IU(BundleId id, Domain p, List<Provided> provides, List<Required> requires) {
			this.id = id;
			this.domain = p;
			this.requires.addAll(requires);
			this.provides.addAll(provides);
		}

		String getRule() {
			return null;
		}

		abstract String getRefType();

		String getName() {
			return domain.get(Constants.BUNDLE_NAME, id.getBsn());
		}

		String getDescription() {
			return domain.get(Constants.BUNDLE_DESCRIPTION);
		}

		String getDescriptionUrl() {
			return domain.getBundleDocURL();
		}

		String getCopyright() {
			return domain.get(Constants.BUNDLE_COPYRIGHT);
		}

		String getLicense() {
			return domain.get(Constants.BUNDLE_LICENSE);
		}

		String getDocUrl() {
			return domain.get(Constants.BUNDLE_DOCURL);
		}

		@Override
		public int compareTo(IU o) {
			return id.compareTo(o.id);
		}

		@Override
		public abstract String toString();

		abstract String getPath();

	}

	/**
	 * Represents a bundle in the requirement graph
	 */
	static class Bundle extends IU {
		final Map<String, String>	attrs;
		final Manifest				manifest;

		Bundle(BundleId id, Domain properties, Map<String, String> attrs, Manifest manifest) {
			super(id, properties, Collections.emptyList(), Collections.emptyList());
			this.attrs = attrs;
			this.manifest = manifest;
			parseManifest(properties);
		}

		private void parseManifest(Domain properties) {
			Parameters imports = properties.getImportPackage();
			for (Map.Entry<String, Attrs> e : imports.entrySet()) {
				String pack = Processor.removeDuplicateMarker(e.getKey());
				boolean optional = isOptional(e.getValue());
				Required r = new Required("java.package", getBundleId(pack, e.getValue()
					.getVersion()), IUType.other, optional);
				requires.add(r);
			}
			Parameters exports = properties.getExportPackage();
			for (Map.Entry<String, Attrs> e : exports.entrySet()) {
				String pack = Processor.removeDuplicateMarker(e.getKey());
				String version = e.getValue()
					.getVersion();
				Provided r = new Provided("java.package", getBundleId(pack, version), IUType.other);
				provides.add(r);
			}
			Entry<String, Attrs> fh = properties.getFragmentHost();
			if (fh != null) {
				Attrs attrs = fh.getValue();
				if (attrs == null) {
					attrs = new Attrs();
				}

				String bsn = fh.getKey();
				String version = attrs.getOrDefault("bundle-version", "0");
				VersionRange range = new VersionRange(version);
				boolean optional = isOptional(attrs);
				provides.add(new Provided("osgi.fragment", getBundleId(fh.getKey(), range.getLeft()
					.toString()), IUType.other));
				requires.add(
					new Required("osgi.bundle", getBundleId(fh.getKey(), range.toString()), IUType.other, optional));
			}
		}

		String getManifest() throws IOException {
			if (manifest != null) {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();

				Manifest manifest = new Manifest(this.manifest);
				manifest.getEntries()
					.clear();
				Jar.writeManifest(manifest, bout);
				String s = new String(bout.toByteArray(), StandardCharsets.UTF_8);
				String lineContinuation = "\r\n ";
				return s.replaceAll(lineContinuation, "");
			}
			return "";
		}

		@Override
		String getPath() {
			return "plugins/" + id.getBsn() + "_" + id.getVersion() + ".jar";
		}

		@Override
		String getRule() {
			return "${repoUrl}/plugins/${id}_${version}.jar";
		}

		@Override
		String getRefType() {
			return "osgi.bundle";
		}

		@Override
		public String toString() {
			return "bundle[id=" + id + "]";
		}

		boolean isSingleton() {
			Entry<String, Attrs> bsn = domain.getBundleSymbolicName();
			if (bsn == null)
				return false;

			String string = bsn.getValue()
				.get(Constants.SINGLETON_DIRECTIVE);
			return Processor.isTrue(string);
		}

		String getProvider() {
			return domain.getBundleVendor();
		}

	}

	/**
	 * A feature in the graph
	 */
	static class Feature extends IU {
		final BundleId	groupId;
		final BundleId	jarId;
		final String	plugin;

		Feature(BundleId id, Domain properties, List<Provided> provides, List<Required> requires, String plugin) {
			super(id, properties, provides, requires);
			this.plugin = plugin;
			this.requires.addAll(requires);
			this.groupId = getBundleId(id.getBsn() + ".feature.group", id.getVersion());
			this.jarId = getBundleId(id.getBsn() + ".feature.jar", id.getVersion());
		}

		String getProvider() {
			return domain.getBundleVendor();
		}

		@Override
		String getPath() {
			return "features/" + id.getBsn() + "_" + id.getVersion() + ".jar";
		}

		@Override
		String getRule() {
			return "${repoUrl}/features/${id}_${version}.jar";
		}

		List<String> getCategory() {
			return Strings.splitQuoted(domain.getBundleCategory());
		}

		@Override
		public String toString() {
			return "feature[id=" + id + "]";
		}

		@Override
		String getRefType() {
			return "org.eclipse.update.feature";
		}

	}

	static class Category extends IU {

		final String	category;
		final String	label;
		final String	description;

		Category(Domain domain, String category, String label, String description, List<Required> features) {
			super(getBundleId("category:" + category, "0.0.0"), domain, Collections.emptyList(), features);
			this.category = category;
			this.label = label;
			this.description = description;
		}

		@Override
		String getRefType() {
			return null;
		}

		@Override
		public String toString() {
			return "category[id=" + id + "]";
		}

		@Override
		String getPath() {
			return null;
		}

		@Override
		String getName() {
			return label;
		}

		@Override
		String getDescription() {
			return description;
		}
	}

	static class Rule {
		final String	filter;
		final String	output;

		Rule(String filter, String output) {
			this.filter = filter;
			this.output = output;
		}
	}

	static class Artifact {
		final IU					iu;
		final Map<String, String>	attributes;
		final Resource				resource;
		final long					length;
		final String				md5;
		final String				sha256;
		final BundleId				actualId;

		Artifact(IU iu, BundleId id, Map<String, String> attrs, Resource resource) throws Exception {
			this.iu = iu;
			this.actualId = id;
			this.attributes = attrs;
			this.resource = resource;

			long length = 0;
			try (InputStream in = resource.openInputStream()) {
				MessageDigest md5 = MessageDigest.getInstance("md5");
				MessageDigest sha256 = MessageDigest.getInstance("sha256");
				InputStream in1 = new DigestInputStream(in, md5);
				InputStream in2 = new DigestInputStream(in1, sha256);
				byte[] buffer = new byte[8192];
				int l;
				while ((l = in2.read(buffer)) != -1) {
					length += l;
				}
				this.length = length;
				this.md5 = Hex.toHexString(md5.digest())
					.toLowerCase();
				this.sha256 = Hex.toHexString(sha256.digest())
					.toLowerCase();
			}
		}

		String md5() {
			return md5;
		}

		Object sha256() {
			return sha256;
		}

		String getPath() {
			return iu.getPath();
		}
	}

	static class Content {
		final Map<String, String>	properties	= new TreeMap<>();
		final List<URI>				references	= new ArrayList<>();
		final List<IU>				units		= new ArrayList<>();
		final String				name;

		Content(String name, List<URI> references, Set<IU> units) {
			this.name = name;
			this.references.addAll(references);
			this.units.addAll(units);
		}
	}

	static class Artifacts {
		final List<Artifact>		artifacts	= new ArrayList<>();
		final Map<String, String>	properties	= new TreeMap<>();
		final List<Rule>			mappings	= new ArrayList<>();
		final String				name;

		Artifacts(String name, List<Rule> mappings, List<Artifact> artifacts) {
			this.name = name;
			this.properties.putAll(properties);
			this.mappings.addAll(mappings);
			this.artifacts.addAll(artifacts);
		}
	}

	final Content						content;
	final Artifacts						artifacts;
	final String						name;
	final Map<String, List<Feature>>	categories;
	final String						update;
	final String						updateLabel;

	P2(String name, Content content, Artifacts artifact, Map<String, List<Feature>> categories, String update,
		String updateLabel) {
		this.content = content;
		this.artifacts = artifact;
		this.name = name;
		this.categories = categories;
		this.update = update;
		this.updateLabel = updateLabel;
	}

	static boolean isOptional(Attrs attrs) {
		if (attrs == null)
			return false;
		String resolution = attrs.get("resolution:");
		return "optional".equals(resolution);
	}

	static BundleId getBundleId(String bsn, String version) {
		VersionRange v = version == null ? new VersionRange("0") : new VersionRange(version);

		return new BundleId(bsn, v.toString());
	}

}
