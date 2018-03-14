package aQute.bnd.deployer.repository;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.ContentNamespace;

import aQute.bnd.deployer.repository.api.CheckResult;
import aQute.bnd.deployer.repository.api.Decision;
import aQute.bnd.deployer.repository.api.IRepositoryContentProvider;
import aQute.bnd.deployer.repository.api.IRepositoryIndexProcessor;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.libg.generics.Create;

public final class RepoResourceUtils {

	private static final int READ_AHEAD_MAX = 5 * 1024 * 1024;

	public static void readIndex(String name, URI baseUri, InputStream stream,
		Collection<IRepositoryContentProvider> contentProviders, IRepositoryIndexProcessor listener, LogService log)
		throws Exception {
		// Make sure we have a buffering stream
		try (InputStream bufferedStream = stream.markSupported() ? stream : new BufferedInputStream(stream)) {
			// Find a compatible content provider for the input
			IRepositoryContentProvider selectedProvider = null;
			IRepositoryContentProvider maybeSelectedProvider = null;
			for (IRepositoryContentProvider provider : contentProviders) {
				CheckResult checkResult;
				try {
					bufferedStream.mark(READ_AHEAD_MAX);
					checkResult = provider.checkStream(name, new ProtectedStream(bufferedStream));
				} finally {
					bufferedStream.reset();
				}

				if (checkResult.getDecision() == Decision.accept) {
					selectedProvider = provider;
					break;
				} else if (checkResult.getDecision() == Decision.undecided) {
					log.log(LogService.LOG_WARNING,
						String.format(
							"Content provider '%s' was unable to determine compatibility with index at URL '%s': %s",
							provider.getName(), baseUri, checkResult.getMessage()));
					if (maybeSelectedProvider == null)
						maybeSelectedProvider = provider;
				}
			}

			// If no provider answered definitively, fall back to the first
			// undecided provider, with an appropriate warning.
			if (selectedProvider == null) {
				if (maybeSelectedProvider != null) {
					selectedProvider = maybeSelectedProvider;
					log.log(LogService.LOG_WARNING,
						String.format(
							"No content provider matches the specified index unambiguously. Selected '%s' arbitrarily.",
							selectedProvider.getName()));
				} else {
					throw new IOException(
						"Invalid repository index: no configured content provider understands the specified index.");
				}
			}

			// Finally, parse the damn file.
			selectedProvider.parseIndex(bufferedStream, baseUri, listener, log);
		}
	}

	public static Capability getIdentityCapability(Resource resource) {
		List<Capability> identityCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identityCaps.isEmpty())
			throw new IllegalArgumentException("Resource has no identity capability.");
		return identityCaps.iterator()
			.next();
	}

	public static String getResourceIdentity(Resource resource) {
		return (String) getIdentityCapability(resource).getAttributes()
			.get(IdentityNamespace.IDENTITY_NAMESPACE);
	}

	public static Version getResourceVersion(Resource resource) {
		Version result;

		Object versionObj = getIdentityCapability(resource).getAttributes()
			.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		if (versionObj == null) {
			result = Version.emptyVersion;
		} else if (versionObj instanceof org.osgi.framework.Version) {
			org.osgi.framework.Version v = (org.osgi.framework.Version) versionObj;
			result = new Version(v.toString());
		} else {
			throw new IllegalArgumentException("Cannot convert to Version from type: " + versionObj.getClass());
		}

		return result;
	}

	public static URI getContentUrl(Resource resource) {
		List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
		if (caps.isEmpty())
			throw new IllegalArgumentException("Resource has no content capability");

		Object uri = caps.iterator()
			.next()
			.getAttributes()
			.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
		if (uri == null)
			throw new IllegalArgumentException("Resource content has no 'uri' attribute.");
		if (uri instanceof URI)
			return (URI) uri;

		try {
			if (uri instanceof URL)
				return ((URL) uri).toURI();
			if (uri instanceof String)
				return new URI((String) uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Failed to convert resource content location to a valid URI.", e);
		}

		throw new IllegalArgumentException("Failed to convert resource content location to a valid URI.");
	}

	public static String getContentSha(Resource resource) {
		List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
		if (caps.isEmpty())
			return null;

		Object contentObj = caps.iterator()
			.next()
			.getAttributes()
			.get(ContentNamespace.CONTENT_NAMESPACE);
		if (contentObj == null)
			return null;
		if (contentObj instanceof String)
			return (String) contentObj;

		throw new IllegalArgumentException("Content attribute is wrong type: " + contentObj.getClass()
			.toString() + " (expected String).");
	}

	public static List<Resource> narrowVersionsByVersionRange(SortedMap<Version, Resource> versionMap,
		String rangeStr) {
		List<Resource> result;
		if (aQute.bnd.osgi.Constants.VERSION_ATTR_LATEST.equals(rangeStr)) {
			Version highest = versionMap.lastKey();
			result = Create.list(versionMap.get(highest));
		} else {
			VersionRange range = rangeStr != null ? new VersionRange(rangeStr) : null;

			// optimisation: skip versions definitely less than the range
			if (range != null && range.getLow() != null)
				versionMap = versionMap.tailMap(range.getLow());

			result = new ArrayList<>(versionMap.size());
			for (Entry<Version, Resource> entry : versionMap.entrySet()) {
				Version version = entry.getKey();
				if (range == null || range.includes(version))
					result.add(entry.getValue());

				// optimisation: skip versions definitely higher than the range
				if (range != null && range.isRange() && version.compareTo(range.getHigh()) >= 0)
					break;
			}
		}
		return result;
	}

}
