package aQute.p2.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.osgi.dto.DTO;
import org.osgi.framework.Version;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.http.HttpClient;
import aQute.lib.collections.MultiMap;
import aQute.lib.exceptions.Exceptions;
import aQute.p2.api.Artifact;
import aQute.p2.api.ArtifactProvider;
import aQute.p2.api.Classifier;
import aQute.p2.provider.Feature.Plugin;

public class TargetImpl implements ArtifactProvider {
	private static final String			FEATURE_GROUP_SUFFIX	= ".feature.group";
	private static final Version		ZERO					= new Version("0");
	final static Logger					logger					= LoggerFactory.getLogger(TargetImpl.class);
	final static DocumentBuilderFactory	dbf						= DocumentBuilderFactory.newInstance();
	final static XPathFactory			xpf						= XPathFactory.newInstance();

	final HttpClient					client;
	final PromiseFactory				promiseFactory;
	final URI							base;

	static class Location extends DTO {
		public URI					repository;
		public Map<String, Version>	units	= new HashMap<>();
		public Classifier			classifier;
	}

	public TargetImpl(HttpClient c, URI base, PromiseFactory promiseFactory) throws Exception {
		this.client = c;
		this.promiseFactory = promiseFactory;
		this.base = normalize(base);
	}

	private URI normalize(URI base) throws Exception {
		String path = base.getPath();
		if (path.endsWith("/"))
			return base;

		return new URI(base.toString() + "/");
	}

	final static Version LAZY = new Version("0.0.0");

	@Override
	public List<Artifact> getAllArtifacts() throws Exception {
		List<Promise<List<Artifact>>> promises = new ArrayList<>();

		List<Location> locations = getLocationsFromTargetPlatformXML(base);
		if (locations.isEmpty()) {
			logger.debug("no locations for {}", base);
		}
		for (Location location : locations) {
			P2Impl p2 = new P2Impl(client, location.repository, promiseFactory);
			Promise<List<Artifact>> submit = promiseFactory.submit(() -> {
				List<Artifact> allArtifacts = p2.getAllArtifacts();
				return filterArtifactsAgainstLocationUnits(location, p2.getAllArtifacts());
			});
			promises.add(submit);
		}

		return promiseFactory.all(promises)
			.getValue()
			.stream()
			.flatMap(l -> l.stream())
			.collect(Collectors.toList());
	}

	/*
	 * use the location units to filter the found bundles. If the unit version
	 * =0.0.0 (LAZY), then we must have the latest version. If the version is
	 * set we require an exact match. If an artifact is a feature we expand it
	 * and add the constituents
	 */

	private List<Artifact> filterArtifactsAgainstLocationUnits(Location location, List<Artifact> artifacts)
		throws Exception {

		logger.debug("Found artifacts {}", artifacts);
		if (artifacts.isEmpty()) {
			logger.info("no artifacts received for {}", base);
		}
		//
		// maps the bsn to the matching or if lazy highest version
		//

		Map<String, Artifact> filtered = new HashMap<>();

		//
		// First perform the selection
		// on the id & version
		// this selects features & bundles

		for (Artifact artifact : artifacts) {

			Version requestedVersion = location.units.get(artifact.id);
			if (requestedVersion == null) {
				// not wanted since it is not in units
				continue;
			}

			if (LAZY.equals(requestedVersion)) {
				Artifact previousArtifact = filtered.get(artifact.id);

				// pick highest possible version if already found
				if (previousArtifact == null || previousArtifact.version.compareTo(artifact.version) < 0) {
					filtered.put(artifact.id, artifact);
					logger.debug("Latest {} {}", artifact.id, artifact.version);
				}
			} else {
				if (requestedVersion.equals(artifact.version)) {
					filtered.put(artifact.id, artifact);
					logger.debug("Exact match {} {}", artifact.id, artifact.version);
				}
			}
		}

		// now we need to expand all the features into their plugins

		logger.debug("Expanding artifacts");

		MultiMap<String, Version> plugins = new MultiMap<>();

		for (Artifact artifact : filtered.values()) {

			if (artifact.classifier != Classifier.FEATURE)
				continue;

			logger.debug("Expanding artifact {}", artifact);

			try {
				File file = client.build()
					.get()
					.useCache()
					.go(artifact.uri);

				Feature f = new Feature(new FileInputStream(file));
				logger.debug("Adding feature {}", f);

				for (Plugin plugin : f.getPlugins()) {
					plugins.add(plugin.id, plugin.version);
				}
			} catch (Exception e) {
				logger.error("failed to create feature {} {}", artifact, e.getMessage());
			}
		}

		//
		// Now we first filter the features from the selected set, this
		// result then still needs the plugins from the features. We use a set
		// because we might have duplicate selections
		//

		Set<Artifact> selectedBundles = filtered.values()
			.stream()
			.filter(artifact -> artifact.classifier == Classifier.BUNDLE)
			.collect(Collectors.toSet());

		logger.debug("selectedBundle {}", selectedBundles);

		//
		// Match the original artifacts against the desired plugins
		// and add any matching bundle to the result.
		//

		nextArtifact: for (Artifact artifact : artifacts) {

			if (artifact.classifier == Classifier.FEATURE)
				continue;

			logger.debug("Artifact to match {} {}", artifact.id, artifact.version);

			List<Version> list = plugins.get(artifact.id);
			if (list == null || list.isEmpty()) {
				logger.debug("bundle not selected in any feature", artifact.id);
				continue;
			}

			logger.debug("bundle selected in a feature", artifact.id);

			for (Version pluginVersion : list) {
				if (pluginVersion.equals(ZERO) || pluginVersion.equals(artifact.version)) {
					logger.debug("Adding bundle {} because feature selects {}", artifact, pluginVersion);
					selectedBundles.add(artifact);
					continue nextArtifact;
				}
			}
		}
		return new ArrayList<>(selectedBundles);
	}

	List<Location> getLocationsFromTargetPlatformXML(URI base) throws Exception {
		try {
			XPath xpath = xpf.newXPath();
			List<Location> locations = new ArrayList<>();

			DocumentBuilder db = dbf.newDocumentBuilder();
			try (InputStream in = client.build()
				.get(InputStream.class)
				.go(base)) {

				Document doc = db.parse(in);

				NodeList list = (NodeList) xpath.evaluate("/target/locations/location", doc, XPathConstants.NODESET);
				for (int i = 0; i < list.getLength(); i++) {
					Location location = new Location();
					Node element = list.item(i);
					String repository = xpath.evaluate("repository/@location", element);
					location.repository = base.resolve(repository)
						.normalize();

					NodeList units = (NodeList) xpath.evaluate("unit", element, XPathConstants.NODESET);
					for (int u = 0; u < units.getLength(); u++) {
						Node unit = units.item(u);
						String bsn = xpath.evaluate("@id", unit);
						bsn = adjustForFeatureSuffix(bsn);
						String version = xpath.evaluate("@version", unit);
						location.units.put(bsn, new Version(version));
					}
					locations.add(location);
				}
			}
			return locations;
		} catch (Exception e) {
			logger.error("Reading platform {} {}", base, e.getMessage());
			throw Exceptions.duck(e);
		}
	}

	//
	// A giant hack ... P2's target platform adds the
	// .feature.group suffix to the
	// id of a feature. I guess they want to see from the name
	// that it is a feature ...
	//
	private String adjustForFeatureSuffix(String bsn) {
		if (!bsn.endsWith(FEATURE_GROUP_SUFFIX))
			return bsn;

		return bsn.substring(0, bsn.length() - FEATURE_GROUP_SUFFIX.length());
	}

}
