package aQute.p2.export;

import static aQute.p2.export.P2.getBundleId;
import static aQute.p2.export.P2.IUType.other;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Manifest;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.BundleNamespace;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.tag.Tag;
import aQute.p2.export.P2.Artifact;
import aQute.p2.export.P2.Artifacts;
import aQute.p2.export.P2.Bundle;
import aQute.p2.export.P2.Category;
import aQute.p2.export.P2.Content;
import aQute.p2.export.P2.Feature;
import aQute.p2.export.P2.IU;
import aQute.p2.export.P2.IUType;
import aQute.p2.export.P2.Provided;
import aQute.p2.export.P2.Required;
import aQute.p2.export.P2.Rule;
import aQute.p2.provider.Signer;

/**
 * An instance of this class is created for each p2 export.
 */
class P2Export {
	final Project				bndrun;
	final Map<String, String>	options;

	final Provided				EQ_TYPE_FEATURE	= new Provided("org.eclipse.equinox.p2.eclipse.type",
		getBundleId("feature", "1.0.0"), other);
	final Provided				EQ_TYPE_BUNDLE	= new Provided("org.eclipse.equinox.p2.eclipse.type",
		getBundleId("bundle", "1.0.0"), other);
	final String				provider;
	final String				update;
	final String				updateLabel;
	final String				name;
	private final boolean		sign;
	private final String		sign_key;
	private final String		sign_passphrase;
	private final String		sign_pubkey;

	/**
	 * Publisher
	 *
	 * @param bndrun the project
	 * @param options any options (name, update, updateLabel, and provider)
	 */
	P2Export(Project bndrun, Map<String, String> options) {
		this.bndrun = bndrun;
		this.options = options;
		this.name = options.getOrDefault("name", bndrun.getName()
			.replaceAll("\\.bndrun$", ".jar"));
		this.update = bndrun.get("update");
		this.updateLabel = bndrun.getProperty("update.label", "Update");
		this.provider = bndrun.getProperty("Bundle-Vendor", "bnd");
		this.sign = Project.isTrue(options.get("sign"));
		this.sign_key = options.get("sign_key");
		this.sign_passphrase = options.get("sign_passphrase");
		this.sign_pubkey = options.get("sign_pubkey");
	}

	Map.Entry<String, Resource> generate() throws Exception {
		P2 p2 = parse();

		if (p2 == null || !bndrun.isOk()) {
			bndrun.check();
			return null;
		}

		@SuppressWarnings("resource")
		Builder builder = new Builder(bndrun);
		Jar jar = builder.build();
		jar.setReproducible("true");
		jar.putResource("content.jar", generateContent(p2));
		jar.putResource("artifacts.jar", generateArtifacts(p2, jar));
		//jar.putResource("p2.index", generateP2Index());
		return new AbstractMap.SimpleEntry<String, Resource>("p2", new JarResource(jar));
	}

	// TODO this p2.index is maybe wrong. it points to content.xml which does
	// not exist.
	// but instead it should look for it in the content.jar
	// the exclamation marks seem to mean "stop searching"
	// private Resource generateP2Index() {
	// // see https://wiki.eclipse.org/Equinox/p2/p2_index
	// String index = """
	// version=1
	// metadata.repository.factory.order=content.xml,\\!
	// artifact.repository.factory.order=artifacts.xml,\\!""";
	// return new EmbeddedResource(index, 0);
	// }

	private Resource generateArtifacts(P2 p2, Jar jar) {

		Signer signer = createSigner();
		String pgp_publicKeys = signer != null ? this.sign_pubkey : null;

		Tag repository = new Tag("repository");
		repository.addAttribute("name", p2.name);
		repository.addAttribute("type", "org.eclipse.equinox.p2.artifact.repository.simpleRepository");
		repository.addAttribute("version", "1");

		properties(repository, //
			"p2.timestamp", System.currentTimeMillis(), //
			"p2.compresssed", true, //
			"pgp.publicKeys", pgp_publicKeys);

		Set<String> addedUpdateUrls = new HashSet<>();
		Tag references = new Tag(repository, "references");
		if (update != null) {
			addedUpdateUrls.add(update);
			new Tag(references, "repository")//
				.addAttribute("uri", update)
				.addAttribute("url", update)
				.addAttribute("type", 1)
				.addAttribute("options", 0);
			new Tag(references, "repository")//
				.addAttribute("uri", update)
				.addAttribute("url", update)
				.addAttribute("type", 0)
				.addAttribute("options", 0);
		}

		p2.content.units.stream()
			.distinct()
			.sorted()
			.forEach(iu -> {

				if (iu instanceof Feature f) {
					if (f.update != null && addedUpdateUrls.add(f.update)) {
						new Tag(references, "repository")//
							.addAttribute("uri", f.update)
							.addAttribute("url", f.update)
							.addAttribute("type", 1)
							.addAttribute("options", 1);
						new Tag(references, "repository")//
							.addAttribute("uri", f.update)
							.addAttribute("url", f.update)
							.addAttribute("type", 0)
							.addAttribute("options", 1);
					}

				}
			});

		size(references);

		Tag mappings = new Tag(repository, "mappings");
		new Tag(mappings, "rule")//
			.addAttribute("filter", "(&(classifier=osgi.bundle))")
			.addAttribute("output", "${repoUrl}/plugins/${id}_${version}.jar");

		new Tag(mappings, "rule")//
			.addAttribute("filter", "(&(classifier=org.eclipse.update.feature))")
			.addAttribute("output", "${repoUrl}/features/${id}_${version}.jar");

		size(mappings);

		Tag artifacts = new Tag(repository, "artifacts");

		for (Artifact a : p2.artifacts.artifacts) {
			String classifier = a.iu.getRefType();
			if (classifier == null) {
				continue;
			}

			Tag artifact = new Tag(artifacts, "artifact") //
				.addAttribute("classifier", classifier)
				.addAttribute("id", a.iu.id.getBsn())
				.addAttribute("version", a.iu.id.getVersion());

			String signature = sign(signer, a);

			Tag properties = properties(artifact, //
				"artifact.size", a.length, //
				"download.size", a.length, //
				"download.md5", a.md5(), //
				"download.checksum.md5", a.md5(), //
				"download.checksum.sha-256", a.sha256(), //
				"pgp.signatures", signature);

			jar.putResource(a.getPath(), a.resource);
		}

		size(artifacts);

		return wrap("artifacts.jar", "artifacts.xml", repository);
	}

	private JarResource generateContent(P2 p2) {

		Tag content = new Tag("repository");
		content.addAttribute("name", name);
		content.addAttribute("type", "org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository");
		content.addAttribute("version", "1.0.0");

		properties(content, //
			"p2.timestamp", System.currentTimeMillis(), //
			"p2.compressed", true);

		Set<String> addedUpdateUrls = new HashSet<>();
		Tag references = new Tag(content, "references");
		if (update != null) {
			addedUpdateUrls.add(update);
			new Tag(references, "repository")//
				.addAttribute("uri", update)
				.addAttribute("url", update)
				.addAttribute("type", 1)
				.addAttribute("options", 0);
			new Tag(references, "repository")//
				.addAttribute("uri", update)
				.addAttribute("url", update)
				.addAttribute("type", 0)
				.addAttribute("options", 0);
		}

		p2.content.units.stream()
			.distinct()
			.sorted()
			.forEach(iu -> {

				if (iu instanceof Feature f) {
					if (f.update != null && addedUpdateUrls.add(f.update)) {
						new Tag(references, "repository")//
							.addAttribute("uri", f.update)
							.addAttribute("url", f.update)
							.addAttribute("type", 1)
							.addAttribute("options", 1);
						new Tag(references, "repository")//
							.addAttribute("uri", f.update)
							.addAttribute("url", f.update)
							.addAttribute("type", 0)
							.addAttribute("options", 1);
					}

				}
			});

		size(references);

		Tag units = new Tag(content, "units");

		p2.content.units.stream()
			.distinct()
			.sorted()
			.forEach(iu -> {

				if (iu instanceof Feature f) {
					doFeatureUnit(units, f);
				} else if (iu instanceof Bundle b) {
					doBundleUnit(units, b);
				} else if (iu instanceof Category c) {
					doCategoryUnit(units, c);
				} else {
					bndrun.error("Unknown unit type %s", iu);
				}
			});

		size(units);

		return wrap("content.jar", "content.xml", content);
	}

	void doFeatureUnit(Tag units, Feature f) {

		{
			Tag groupUnit = new Tag(units, "unit")//
				.addAttribute("id", f.groupId.getBsn()) //
				.addAttribute("version", f.id.getVersion())//
				.addAttribute("singleton", "false");

			update(groupUnit, f.groupId);

			Tag ps = properties(groupUnit, //
				"org.eclipse.equinox.p2.name", f.getName(), //
				"org.eclipse.equinox.p2.description", f.getDescription(), //
				"org.eclipse.equinox.p2.description.url", f.getDocUrl(), //
				"org.eclipse.equinox.p2.provider", provider, //
				"org.eclipse.equinox.p2.type.group", true);

			Tag provides = new Tag(groupUnit, "provides");
			provided(provides, "org.eclipse.equinox.p2.iu", f.groupId);

			Tag requires = new Tag(groupUnit, "requires");
			for (Required k : f.requires) {
				required(requires, k);
			}
			Tag required = required(requires,
				new Required("org.eclipse.equinox.p2.iu", toExact(f.jarId), IUType.other, false));
			new Tag(required, "filter", "(org.eclipse.update.install.features=true)");

			doLegal(groupUnit, f);

			size(requires, "namespace", "name", "version");
			size(provides, "namespace", "name", "version");
			size(ps);
		}

		{
			Tag jarUnit = new Tag(units, "unit")//
				.addAttribute("id", f.jarId.getBsn()) //
				.addAttribute("version", f.id.getVersion())//
				.addAttribute("singleton", "false");

			Tag ps = properties(jarUnit, //
				"org.eclipse.equinox.p2.name", f.getName(), //
				"org.eclipse.equinox.p2.description", f.getDescription(), //
				"org.eclipse.equinox.p2.description.url", f.getDocUrl(), //
				"org.eclipse.equinox.p2.provider", provider, //
				"org.eclipse.update.feature.plugin", f.plugin);

			Tag provides = new Tag(jarUnit, "provides");
			provided(provides, "org.eclipse.equinox.p2.iu", f.jarId);
			provided(provides, EQ_TYPE_FEATURE);
			provided(provides, "org.eclipse.update.feature", f.id);

			new Tag(jarUnit, "filter", "(org.eclipse.update.install.features=true)");
			Tag artifacts = new Tag(jarUnit, "artifacts");
			artifact(artifacts, "org.eclipse.update.feature", f.id);

			touchpoint(jarUnit, "org.eclipse.equinox.p2.osgi", "zipped", true);
			doLegal(jarUnit, f);

			size(ps);
			size(provides, "namespace", "name", "version");
			size(artifacts);
		}

	}

	void doBundleUnit(Tag units, Bundle b) {
		try {
			Tag unit = new Tag(units, "unit")//
				.addAttribute("id", b.id.getBsn()) //
				.addAttribute("version", b.id.getVersion())//
				.addAttribute("singleton", b.isSingleton());

			update(unit, b.id);

			Tag properties = properties(unit, //
				"org.eclipse.equinox.p2.name", b.getName(), //
				"org.eclipse.equinox.p2.description", b.getDescription(), //
				"org.eclipse.equinox.p2.description.url", b.getDocUrl(), //
				"org.eclipse.equinox.p2.provider", provider, //
				"org.eclipse.equinox.p2.doc.url", b.getDocUrl());

			Tag provides = new Tag(unit, "provides");
			provided(provides, "org.eclipse.equinox.p2.iu", b.id);
			provided(provides, "osgi.bundle", b.id);

			for (Provided k : b.provides) {
				provided(provides, k);
			}
			provided(provides, EQ_TYPE_BUNDLE);

			Tag requires = new Tag(unit, "requires");

			for (Required k : b.requires) {
				required(requires, k);
			}

			Tag artifacts = new Tag(unit, "artifacts");
			artifact(artifacts, "osgi.bundle", b.id);

			touchpoint(unit, "org.eclipse.equinox.p2.osgi", "manifest", b.getManifest());

			size(requires, "namespace", "name", "version");
			size(provides, "namespace", "name", "version");
			size(artifacts);
		} catch (Exception e) {
			bndrun.error("failed to create unit for %s: %s", b, e, e);
		}
	}

	void doCategoryUnit(Tag units, Category c) {

		Tag unit = new Tag(units, "unit")//
			.addAttribute("id", c.id.getBsn()) //
			.addAttribute("version", c.id.getVersion())//
			.addAttribute("singleton", "false");

		Tag ps = properties(unit, //
			"org.eclipse.equinox.p2.name", c.getName(), //
			"org.eclipse.equinox.p2.description", c.getDescription(), //
			"org.eclipse.equinox.p2.description.url", c.getDocUrl(), //
			"org.eclipse.equinox.p2.type.category", true);

		Tag provides = new Tag(unit, "provides");
		provided(provides, "org.eclipse.equinox.p2.iu", c.id);

		Tag requires = new Tag(unit, "requires");

		c.requires.stream()
			.filter(r -> r.type == IUType.feature)
			.map(Required.class::cast)
			.forEach(r -> {
				required(requires, r);
			});

		size(ps);
		size(requires, "namespace", "name", "version");
		size(provides, "namespace", "name", "version");

	}

	private void touchpoint(Tag unit, String type, String key, Object v) {
		new Tag(unit, "touchpoint")//
			.addAttribute("id", type)//
			.addAttribute("version", "1.0.0");

		Tag touchpointData = new Tag(unit, "touchpointData");
		Tag instructions = new Tag(touchpointData, "instructions");
		Tag instruction = new Tag(instructions, "instruction", v).addAttribute("key", key);

		size(instructions);
		size(touchpointData);
	}

	private void update(Tag parent, BundleId id) {
		Tag update = new Tag(parent, "update");
		update.addAttribute("id", id.getBsn());
		update.addAttribute("range", toUpdateRange(id.getVersion()));
		update.addAttribute("severity", "0");
	}

	@SuppressWarnings("unused")
	private VersionRange getExactRange(String version) {
		Version low = new Version(version);
		Version high = new Version(version);
		return new VersionRange('[', low, high, ']');
	}

	private void artifact(Tag parent, String namespace, BundleId id) {
		new Tag(parent, "artifact")//
			.addAttribute("classifier", namespace) //
			.addAttribute("id", id.getBsn())//
			.addAttribute("version", id.getVersion());
	}

	private void provided(Tag requires, String namespace, BundleId r) {
		new Tag(requires, "provided")//
			.addAttribute("namespace", namespace)//
			.addAttribute("name", r.getBsn())//
			.addAttribute("version", r.getVersion());
	}

	private void provided(Tag provides, Provided p) {
		provided(provides, p.namespace, p.id);
	}

	private Tag required(Tag requires, Required r) {
		return required(requires, r.namespace, r.id, r.optional);
	}

	private Tag required(Tag requires, String namespace, BundleId id, boolean optional) {
		Tag required = new Tag(requires, "required")//
			.addAttribute("namespace", namespace)//
			.addAttribute("name", id.getBsn())//
			.addAttribute("range", id.getVersion());

		if (optional) {
			required.addAttribute("optional", true);
		}
		return required;
	}

	private Artifact generateFeature(Feature feature) throws Exception {
		// Feature schema: It was a bit hard to find:
		// https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fproduct_def_feature.htm
		// and
		// https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Ffeature_manifest.html
		// and
		// https://archive.eclipse.org/eclipse/downloads/documentation/2.0/html/plugins/org.eclipse.platform.doc.isv/reference/misc/feature_archive.html


		Tag f = new Tag("feature");
		f.addAttribute("id", feature.id.getBsn());
		f.addAttribute("version", feature.id.getVersion());
		f.addAttribute("label", feature.getName());
		f.addAttribute("provider-name", provider);

		doDescription(f, feature);
		doLegalFeature(f, feature);

		Tag url = new Tag(f, "url");

		if (feature.update != null) {
			Tag tUpdate = new Tag(url, "update");
			tUpdate.addAttribute("url", feature.update);
			if (feature.updateLabel != null)
				tUpdate.addAttribute("label", feature.updateLabel);

			Tag tDiscovery = new Tag(url, "discovery");
			tDiscovery.addAttribute("url", feature.update);
			if (feature.updateLabel != null)
				tDiscovery.addAttribute("label", feature.updateLabel);
		}

		Tag requires = new Tag(f, "requires");

		for (Required require : feature.requires) {
			if (require.type == IUType.feature) {

				new Tag(requires, "import")//
					.addAttribute("feature", require.id.getBsn())//
					.addAttribute("version", require.range.getLeft()) //
					.addAttribute("match", getMatch(require.range));
			}
		}

		for (Required require : feature.requires) {
			if (require.type == IUType.bundle) {
				new Tag(f, "plugin")//
					.addAttribute("id", require.id.getBsn())//
					.addAttribute("version", require.range) //
					.addAttribute("unpack", false);
			}
		}

		JarResource wrap = wrap(feature.id.getBsn() + ".jar", "feature.xml", f);
		return new Artifact(feature, feature.groupId, Collections.emptyMap(), wrap);
	}

	/**
	 * The import element within a requires element in a feature.xml file is
	 * used to express a dependency on another plug-in or feature. The match
	 * attribute of the import element is used to specify the type of version
	 * matching that should be used when resolving this dependency. Here are the
	 * possible values for the match attribute:
	 * <ul>
	 * <li>perfect: The version of the dependency must match exactly the version
	 * specified in the version attribute of the import element.
	 * <li>equivalent: The major and minor parts of the version must match
	 * exactly. The service level (third segment) of the dependency's version
	 * must be greater or equal to the one specified.
	 * <li>compatible: The major part of the version must match exactly. The
	 * minor part of the dependency's version must be greater or equal to the
	 * one specified. This is the default value if the match attribute is not
	 * specified.
	 * <li>greaterOrEqual: The version of the dependency must be greater or
	 * equal to the one specified. perfectOrGreater: The version must either
	 * match perfectly or be greater than the one specified.
	 * </ul>
	 *
	 * @param range the version range
	 * @return the match value
	 */
	private String getMatch(VersionRange range) {
		Version low = range.getLeft();
		Version high = range.getRight();
		if (high == null)
			high = low;

		// perfect requires [1.2.3,1.2.3]
		if (low.equals(high) && range.getLeftType() == VersionRange.LEFT_CLOSED
			&& range.getRightType() == VersionRange.RIGHT_CLOSED)
			return "perfect";

		// equivalent
		if (low.getMajor() == high.getMajor() && low.getMinor() == high.getMinor())
			return "equivalent";

		// compatible
		if (low.getMajor() + 1 == high.getMajor() && range.getLeftType() == VersionRange.LEFT_CLOSED
			&& range.getRightType() == VersionRange.RIGHT_OPEN)
			return "compatible";

		return "greaterOrEqual";
	}

	/**
	 * -export p2;features="a.bndrun,b.bndrun";output=target/p2
	 * <p>
	 * In bndrun
	 * <p>
	 * -p2.feature.required = org.eclipse.platform;type=feature;version=4.25
	 *
	 * @param reporter
	 * @param name
	 * @param options
	 * @param workspace
	 * @return
	 */
	P2 parse() {
		List<String> featurePaths = Strings.splitQuoted(bndrun.get("features"));

		MultiMap<String, Feature> categories = new MultiMap<>();
		Map<String, Feature> features = new LinkedHashMap<>();
		Map<BundleId, Bundle> bundles = new LinkedHashMap<>();
		List<URI> references = new ArrayList<>();
		Set<IU> units = new TreeSet<>();
		List<Artifact> artifacts = new ArrayList<>();
		List<Rule> mappings = new ArrayList<>();

		for (String featurePath : featurePaths) {

			File featureFile = bndrun.getFile(featurePath);
			try {

				if (!featureFile.isFile()) {
					bndrun.error("no such feature file %s", featureFile);
					continue;
				}

				Run definition = Run.createRun(bndrun.getWorkspace(), featureFile);
				definition.setParent(bndrun);

				BundleId featureId = getFeatureId(definition);

				List<Required> requires = new ArrayList<>();
				List<Provided> provides = new ArrayList<>();

				for (Container c : definition.getRunbundles()) {
					if (c.getError() != null) {
						bndrun.error("Feature %s dependency %s has an error", featureFile, c);
					} else {
						BundleId bundleId = c.getBundleId();
						Bundle b = bundles.computeIfAbsent(bundleId, k -> {
							try {
								return new Bundle(k, Domain.domain(c.getManifest()), c.getAttributes(),
									c.getManifest());
							} catch (Exception e) {
								bndrun.error("failed to retrieve manifest from bundle %s: %s", c, e);
								return null;
							}
						});
						units.add(b);
						BundleId range = toExact(bundleId);
						Map<String, String> attributes = c.getAttributes();
						boolean optional = attributes != null
							? BundleNamespace.RESOLUTION_OPTIONAL
								.equals(attributes.get(BundleNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE))
							: false;
						requires.add(new Required("org.eclipse.equinox.p2.iu", range, IUType.bundle, optional));
						Artifact a = new Artifact(b, b.id, c.getAttributes(), new FileResource(c.getFile()));
						artifacts.add(a);
					}
				}

				VersionRange defaultRange = exact(new Version(featureId.getVersion()));
				parseRequired(requires, definition, defaultRange.toString());
				parseProvided(provides, definition);

				Feature feature = new Feature(featureId, definition, provides, requires, definition.get("-p2.plugin"),
					definition.get("update"), definition.get("update.label"));
				units.add(feature);

				Artifact art = generateFeature(feature);
				artifacts.add(art);

				for (String category : feature.getCategory()) {
					categories.add(category, feature);
				}

				bndrun.getInfo(definition, featureFile.getName() + ": ");
			} catch (FileNotFoundException e) {} catch (Exception e) {
				bndrun.error("%s", e.getMessage());
			}
		}

		Parameters categoryDescriptions = new Parameters(bndrun.mergeProperties("-categories"));

		categories.forEach((k, v) -> {
			List<Required> requires = v.stream()
				.map(f -> new Required("org.eclipse.equinox.p2.iu", f.groupId, IUType.feature, false))
				.toList();

			Attrs attrs = categoryDescriptions.getOrDefault(k, new Attrs());
			String label = attrs.get("label", k);
			String description = attrs.get("description", "");
			Category c = new Category(bndrun, k, label, description, requires);
			units.add(c);
		});
		Artifacts artifact = new Artifacts(name, mappings, artifacts);
		Content content = new P2.Content(name, references, units);
		return new P2(name, content, artifact, categories, "", "");
	}

	private void parseRequired(List<Required> prs, Processor definition, String defaultRange) {
		String required = definition.mergeProperties(Constants.REQUIRE_CAPABILITY);
		Parameters pars = new Parameters(required);
		for (Map.Entry<String, Attrs> e : pars.entrySet())
			try {
				String namespace = Processor.removeDuplicateMarker(e.getKey());
				Attrs attrs = e.getValue();
				String range = attrs.getVersion();
				if (range == null) {
					range = defaultRange;
				}
				boolean optional = P2.isOptional(attrs);
				String bsn = attrs.get("name");
				BundleId id = getBundleId(bsn, range);

				switch (namespace) {
					case "feature" :
						BundleId groupId = new BundleId(id.getBsn() + ".feature.group", id.getVersion());
						prs.add(new Required("org.eclipse.equinox.p2.iu", groupId, IUType.feature, optional));
						break;

					case "osgi.wiring.bundle" :
					case "bundle" :
						prs.add(new Required("org.eclipse.equinox.p2.iu", toExact(id), IUType.bundle, optional));
						break;

					case "osgi.wiring.package" :
					case "package" :
					case "java.package" :
						prs.add(new Required("java.package", toCompatible(id), IUType.other, optional));
						break;

					default :
						prs.add(new Required(namespace, id, IUType.other, optional));
						break;
				}
			} catch (Exception ee) {
				ee.printStackTrace();
			}
	}

	private void parseProvided(List<Provided> prs, Processor definition) {
		String provided = definition.mergeProperties(Constants.PROVIDE_CAPABILITY);
		Parameters pars = new Parameters(provided);
		for (Map.Entry<String, Attrs> e : pars.entrySet())
			try {
				String namespace = Processor.removeDuplicateMarker(e.getKey());
				Attrs attrs = e.getValue();
				String version = attrs.getVersion();
				String bsn = attrs.get("name");
				BundleId id = getBundleId(bsn, version);

				switch (namespace) {
					case "feature" :
						prs.add(new Provided("org.eclipse.equinox.p2.iu", id, IUType.feature));
						break;
					case "bundle" :
						prs.add(new Provided("org.eclipse.equinox.p2.iu", id, IUType.bundle));
						break;
					default :
						prs.add(new Provided(namespace, id, IUType.other));
						break;
				}
			} catch (Exception ee) {
				ee.printStackTrace();
			}
	}

	private Tag properties(Tag parent, Object... pairs) {
		Tag properties = new Tag(parent, "properties");
		for (int i = 0; i < pairs.length; i += 2) {
			String key = (String) pairs[i];
			Object value = pairs[i + 1];
			if (value != null) {
				new Tag(properties, "property").addAttribute("name", key)
					.addAttribute("value", value);
			}
		}
		size(properties);
		return properties;
	}

	private void size(Tag tag, String... sorters) {

		List<Object> contents = tag.getContents();
		if (sorters.length > 0) {
			Collections.sort(contents, (a, b) -> {
				if (a instanceof Tag aa && b instanceof Tag bb) {
					for (String sorter : sorters) {
						String aaa = aa.getAttribute(sorter);
						String bbb = bb.getAttribute(sorter);
						if (aaa == bbb)
							continue;

						if (aaa == null)
							return -1;
						if (bbb == null)
							return 1;

						int result = aaa.compareTo(bbb);
						if (result == 0)
							continue;
						return result;
					}
				}
				return 0;
			});
		}
		int size = contents.size();
		if (size == 0)
			tag.remove();
		else
			tag.addAttribute("size", size);
	}

	private JarResource wrap(String jarName, String xmlName, Tag xml) {
		Jar jar = new Jar(jarName);
		jar.setReproducible("true");
		jar.setManifest((Manifest) null);
		jar.setDoNotTouchManifest();

		jar.putResource(xmlName, toResource(xml));
		return new JarResource(jar);
	}

	private Resource toResource(Tag xml) {
		byte[] bytes = xml.toBytes();
		return new EmbeddedResource(bytes, 0);
	}

	private String toUpdateRange(String version) {
		return "[0.0.0," + version + ")";
	}

	private void doDescription(Tag f, IU feature) {
		Tag description = new Tag(f, "description", feature.getDescription());
		description.setCDATA();

		String url = feature.getDescriptionUrl();
		if (url != null)
			description.addAttribute("url", url);
	}

	/**
	 * Adds license information for a <unit> inside content.xml (Note: this is
	 * different than {@link #doLegalFeature(Tag, IU)})
	 */
	private void doLegal(Tag u, IU unit) {
		if (unit.getLicense() != null) {
			Tag licenses = new Tag(u, "licenses");
			Parameters ls = new Parameters(unit.getLicense());

			ls.forEach((k, v) -> {
				Tag license = new Tag(licenses, "license");
				license.setCDATA();

				String link = v.get("link");
				String description = v.get("description");

				// better than description
				// for large files
				String licensefilename = v.get("file");

				license.addAttribute("url", link != null ? link : k);
				license.addAttribute("uri", link != null ? link : k);

				if (licensefilename != null) {
					try {
						license.addContent(IO.collect(bndrun.getFile(licensefilename)));
					} catch (IOException e) {
						license.addContent("Error: Cannot read license file: " + licensefilename);
					}
				} else {
					license.addContent(description != null ? description : v.toString());
				}

			});
			size(licenses);
		}
		if (unit.getCopyright() != null) {
			Tag copyright = new Tag(u, "copyright", unit.getCopyright());
			copyright.setCDATA();

		}

	}

	/**
	 * Adds license information inside feature.xml
	 */
	private void doLegalFeature(Tag f, IU feature) {

		if (feature.getCopyright() != null) {
			Tag copyright = new Tag(f, "copyright", feature.getCopyright());
			copyright.setCDATA();
		}

		if (feature.getLicense() != null) {
			Tag license = new Tag(f, "license");
			license.setCDATA();

			Parameters ls = new Parameters(feature.getLicense());
			Entry<String, Attrs> first = ls.stream()
				.findFirst()
				.orElse(null);
			String k = first.getKey();
			Attrs v = first.getValue();

			String link = v.get("link");
			String description = v.get("description");

			// better than description
			// for large files
			String licensefilename = v.get("file");

			license.addAttribute("url", link != null ? link : k);

			if (licensefilename != null) {
				try {
					license.addContent(IO.collect(bndrun.getFile(licensefilename)));
				} catch (IOException e) {
					license.addContent("Error: Cannot read license file: " + licensefilename);
				}
			} else {
				license.addContent(description != null ? description : v.toString());
			}
		}
	}

	private BundleId getFeatureId(Processor definition) {
		Entry<String, Attrs> entry = definition.getBundleSymbolicName();

		String featureName;
		if (entry != null) {
			featureName = entry.getKey();
		} else {
			featureName = definition.getPropertiesFile()
				.getName()
				.replaceAll("\\.bndrun$", "");
		}

		String version = definition.getBundleVersion();
		if (version == null) {
			version = bndrun.getBundleVersion();
			if (version == null) {
				version = "0.0.0";
			}
		}

		version = Builder.doSnapshot(version, definition.get(Constants.SNAPSHOT));
		return getBundleId(featureName, version);
	}

	private VersionRange exact(Version low) {
		return new VersionRange('[', low, low, ']');
	}

	private VersionRange compatible(Version low) {
		Version high = new Version(low.getMajor() + 1, 0, 0);
		return new VersionRange('[', low, high, ')');
	}

	private BundleId toExact(BundleId id) {
		return new BundleId(id.getBsn(), toExact(id.getVersion()));
	}

	private BundleId toCompatible(BundleId id) {
		return new BundleId(id.getBsn(), toCompatible(id.getVersion()));
	}

	private String toExact(String version) {
		if (version == null)
			version = "0.0.0";

		VersionRange r = VersionRange.valueOf(version);
		boolean hasupperbound = r.getRight() != null;
		if (hasupperbound)
			return r.toString();

		return exact(r.getLeft()).toString();
	}

	private String toCompatible(String version) {
		if (version == null)
			version = "0.0.0";

		VersionRange r = VersionRange.valueOf(version);
		boolean hasupperbound = r.getRight() != null;
		if (hasupperbound)
			return r.toString();

		return compatible(r.getLeft()).toString();
	}

	private Signer createSigner() {
		if (!sign) {
			return null;
		}
		return FakeSigner.BND_FAKE_PGP_KEY.equals(sign_key) ? new FakeSigner(null, null, null)
			: new Signer(sign_key, sign_passphrase, bndrun.getProperty("gpg", "gpg"));
	}


	private static String sign(Signer signer, Artifact a) {
		if (signer == null) {
			return null;
		}

		if (a.resource instanceof FileResource res) {
			File archiveFile = res.getFile();
			try {
				return new String(signer.sign(archiveFile));
			} catch (Exception e) {
				return null;
			}
		}
		else if (a.resource instanceof JarResource res) {
			File tmp = null;
			try {
				tmp = Files.createTempFile(res.getJar()
					.getName(), "")
					.toFile();

				res.write(tmp);
				return new String(signer.sign(tmp));
			} catch (Exception e) {
				return null;
			}
			finally {
				if (tmp != null) {
					IO.delete(tmp);
				}
			}
		}
		return null;
	}

	private final static class FakeSigner extends Signer {

		/**
		 * reserved magic keyname for unit tests
		 */
		static final String BND_FAKE_PGP_KEY = "__BND_FAKE_PGP_KEY__";

		public FakeSigner(String key, String passphrase, String cmd) {
			super(key, passphrase, cmd);
		}

		@Override
		public byte[] sign(File f) throws Exception {
			return new String("FAKE SIGNATURE").getBytes(StandardCharsets.UTF_8);
		}

	}
}
