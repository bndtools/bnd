package org.bndtools.templating.load;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.bndtools.templating.BytesResource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.service.repository.ContentNamespace;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;

public class CapabilityBasedTemplate implements Template {

	private static final String DEFAULT_DIR = "template/";

	private final Capability capability;
	private final Workspace workspace;

	private final String name;
	private final String category;
	private final String description;
	private final Version version;
	
	private final String dir;
	private final URI iconUri;
	
	private final String helpPath;
	
	private File _bundleFile = null;
	
	public CapabilityBasedTemplate(Capability capability, Workspace workspace) {
		this.capability = capability;
		this.workspace = workspace;
		
		Map<String, Object> attrs = capability.getAttributes();

		Object nameObj = attrs.get("name");
		this.name = nameObj instanceof String ? (String) nameObj : "<<unknown>>";
		
		this.description = "from " + ResourceUtils.getIdentityCapability(capability.getResource()).osgi_identity();

		Object categoryObj = attrs.get("category");
		category = categoryObj instanceof String ? (String) categoryObj : null;
		
		// Get version from the capability if found, otherwise it comes from the bundle
		Object versionObj = attrs.get("version");
		if (versionObj instanceof Version)
			this.version = (Version) versionObj;
		else if (versionObj instanceof String)
			this.version = Version.parseVersion((String) versionObj);
		else {
			String v = ResourceUtils.getIdentityVersion(capability.getResource());
			this.version = v != null ? Version.parseVersion(v) : Version.emptyVersion;
		}

		Object dirObj = attrs.get("dir");
		if (dirObj instanceof String) {
			String dirStr = ((String) dirObj).trim();
			if (dirStr.charAt(dirStr.length() - 1) != '/')
				dirStr += '/';
			this.dir = dirStr;
		} else {
			this.dir = DEFAULT_DIR;
		}
		
		Object iconObj = attrs.get("icon");
		iconUri = iconObj instanceof String ? URI.create((String) iconObj) : null;
		
		Object helpObj = attrs.get("help");
		helpPath = (String) (helpObj instanceof String ? helpObj : null);
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getCategory() {
		return category;
	}
	
	@Override
	public String getShortDescription() {
		return description;
	}
	
	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public int getRanking() {
		Object rankingObj = capability.getAttributes().get("ranking");
		return rankingObj instanceof Number ? ((Number) rankingObj).intValue() : 0;
	}

	@Override
	public ResourceMap getInputSources() throws IOException {
		File bundleFile = fetchBundle();
		
		ResourceMap map = new ResourceMap();
		try (JarInputStream in = new JarInputStream(new FileInputStream(bundleFile))) {
			JarEntry jarEntry = in.getNextJarEntry();
			while (jarEntry != null) {
				String entryPath = jarEntry.getName();
				if (!entryPath.endsWith("/")) { //ignore directory entries
					if (entryPath.startsWith(dir)) {
						String relativePath = entryPath.substring(dir.length());

						// cannot use IO.collect() because it closes the whole JarInputStream
						BytesResource resource = BytesResource.loadFrom(in);
						map.put(relativePath, resource);
					}
				}
				jarEntry = in.getNextJarEntry();
			}
		}
		return map;
	}
	
	@Override
	public URI getIcon() {
		return iconUri;
	}
	
	@Override
	public URI getHelpContent() {
		URI uri = null;
		if (helpPath != null) {
			try {
				File f = fetchBundle();
				uri = new URI("jar:" + f.toURI().toURL() + "!/" + helpPath);
			} catch (Exception e) {
				// ignore
			}
		}
		return uri;
	}

	private synchronized File fetchBundle() throws IOException {
		if (_bundleFile != null)
			return _bundleFile;

		Capability idCap = capability.getResource().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
		String id = (String) idCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);

		Capability contentCap = capability.getResource().getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
		URI location;
		Object locationObj = contentCap.getAttributes().get("url");
		if (locationObj instanceof URI)
			location = (URI) locationObj;
		else if (locationObj instanceof String)
			location = URI.create((String) locationObj);
		else
			throw new IOException("Template repository entry is missing url attribute");

		if ("file".equals(location.getScheme())) {
			_bundleFile = IO.getFile(location.getPath());
			return _bundleFile;
		}
		
		String hashStr = (String) contentCap.getAttributes().get(ContentNamespace.CONTENT_NAMESPACE);
		
		Map<String, String> searchProps = new HashMap<>();
		searchProps.put("version", "hash");
		searchProps.put("hash", "SHA-256:" + hashStr);
		
		List<RepositoryPlugin> repoPlugins = workspace.getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin plugin : repoPlugins) {
			try {
				File file = plugin.get(id, null, searchProps);
				if (file != null) {
					this._bundleFile = file;
					return _bundleFile;
				}
			} catch (Exception e) {
				// ignore
			}
		}

		throw new IOException("Unable to fetch bundle for template: " + getName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((capability == null) ? 0 : capability.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CapabilityBasedTemplate other = (CapabilityBasedTemplate) obj;
		if (capability == null) {
			if (other.capability != null)
				return false;
		} else if (!capability.equals(other.capability))
			return false;
		return true;
	}
	
	
	
}
