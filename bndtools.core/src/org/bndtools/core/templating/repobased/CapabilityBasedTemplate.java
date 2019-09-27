package org.bndtools.core.templating.repobased;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.apache.felix.metatype.AD;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.felix.metatype.OCD;
import org.bndtools.templating.BytesResource;
import org.bndtools.templating.FolderResource;
import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateEngine;
import org.bndtools.templating.util.AttributeDefinitionImpl;
import org.bndtools.templating.util.CompositeOCD;
import org.bndtools.templating.util.ObjectClassDefinitionImpl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.service.repository.ContentNamespace;

import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.lib.io.IO;
import bndtools.Plugin;

public class CapabilityBasedTemplate implements Template {

	private static final String		DEFAULT_DIR		= "template/";

	private final Capability		capability;
	private final BundleLocator		locator;
	private final TemplateEngine	engine;

	private final String			name;
	private final String			category;
	private final String			description;
	private final Version			version;

	private final String			dir;
	private final URI				iconUri;

	private final String			metaTypePath;
	private final String			ocdRef;

	private final String			helpPath;

	private File					_bundleFile		= null;
	private ResourceMap				_inputResources	= null;

	public CapabilityBasedTemplate(Capability capability, BundleLocator locator, TemplateEngine engine) {
		this.capability = capability;
		this.locator = locator;
		this.engine = engine;

		Map<String, Object> attrs = capability.getAttributes();

		Object nameObj = attrs.get("name");
		this.name = nameObj instanceof String ? (String) nameObj : "<<unknown>>";

		this.description = "from " + ResourceUtils.getIdentityCapability(capability.getResource())
			.osgi_identity();

		Object categoryObj = attrs.get("category");
		category = categoryObj instanceof String ? (String) categoryObj : null;

		// Get version from the capability if found, otherwise it comes from the
		// bundle
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
		helpPath = helpObj instanceof String ? (String) helpObj : null;

		Object metaTypeObj = attrs.get("metaType");
		metaTypePath = metaTypeObj instanceof String ? (String) metaTypeObj : null;

		Object ocdObj = attrs.get("ocd");
		ocdRef = ocdObj instanceof String ? ((String) ocdObj).trim() : null;
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
		Object rankingObj = capability.getAttributes()
			.get("ranking");
		return rankingObj instanceof Number ? ((Number) rankingObj).intValue() : 0;
	}

	@Override
	public ObjectClassDefinition getMetadata() throws Exception {
		return getMetadata(new NullProgressMonitor());
	}

	@Override
	public ObjectClassDefinition getMetadata(IProgressMonitor monitor) throws Exception {
		String resourceId = ResourceUtils.getIdentityCapability(capability.getResource())
			.osgi_identity();

		final CompositeOCD compositeOcd = new CompositeOCD(name, description, null);

		if (metaTypePath != null) {
			try (JarFile bundleJarFile = new JarFile(fetchBundle())) {
				JarEntry metaTypeEntry = bundleJarFile.getJarEntry(metaTypePath);
				try (InputStream entryInput = bundleJarFile.getInputStream(metaTypeEntry)) {
					MetaData metaData = new MetaDataReader().parse(entryInput);

					@SuppressWarnings("rawtypes")
					Map ocdMap = metaData.getObjectClassDefinitions();
					if (ocdMap != null) {
						if (ocdMap.size() == 1) {
							@SuppressWarnings("unchecked")
							Entry<String, OCD> entry = (Entry<String, OCD>) ocdMap.entrySet()
								.iterator()
								.next();
							// There is exactly one OCD, but if the capability
							// specified the 'ocd' property then it must
							// match.
							if (ocdRef == null || ocdRef.equals(entry.getKey())) {
								compositeOcd.addDelegate(new FelixOCDAdapter(entry.getValue()));
							} else {
								log(IStatus.WARNING, String.format(
									"MetaType entry '%s' from resource '%s' did not contain an Object Class Definition with id '%s'",
									metaTypePath, resourceId, ocdRef), null);
							}
						} else {
							// There are multiple OCDs in the MetaType record,
							// so the capability must have specified the
							// 'ocd' property.
							if (ocdRef != null) {
								OCD felixOcd = (OCD) ocdMap.get(ocdRef);
								if (felixOcd != null) {
									compositeOcd.addDelegate(new FelixOCDAdapter(felixOcd));
								} else {
									log(IStatus.WARNING, String.format(
										"MetaType entry '%s' from resource '%s' did not contain an Object Class Definition with id '%s'",
										metaTypePath, resourceId, ocdRef), null);
								}
							} else {
								log(IStatus.WARNING, String.format(
									"MetaType entry '%s' from resource '%s' contains multiple Object Class Definitions, and no 'ocd' property was specified.",
									metaTypePath, resourceId), null);
							}
						}
					}
				}
			}
		}

		// Add attribute definitions for any parameter names found in the
		// templates and not already
		// loaded from the Metatype XML.
		ObjectClassDefinitionImpl ocdImpl = new ObjectClassDefinitionImpl(name, description, null);
		ResourceMap inputs = getInputSources();
		Map<String, String> params = engine.getTemplateParameters(inputs, monitor);
		for (Entry<String, String> entry : params.entrySet()) {
			AttributeDefinitionImpl ad = new AttributeDefinitionImpl(entry.getKey(), entry.getKey(), 0,
				AttributeDefinition.STRING);
			if (entry.getValue() != null)
				ad.setDefaultValue(new String[] {
					entry.getValue()
				});
			ocdImpl.addAttribute(ad, true);
		}
		compositeOcd.addDelegate(ocdImpl);

		return compositeOcd;
	}

	@Override
	public ResourceMap generateOutputs(Map<String, List<Object>> parameters) throws Exception {
		return generateOutputs(parameters, new NullProgressMonitor());
	}

	@Override
	public ResourceMap generateOutputs(Map<String, List<Object>> parameters, IProgressMonitor monitor)
		throws Exception {
		ResourceMap inputs = getInputSources();
		return engine.generateOutputs(inputs, parameters, monitor);
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
				uri = new URI("jar:" + f.toURI()
					.toURL() + "!/" + helpPath);
			} catch (Exception e) {
				// ignore
			}
		}
		return uri;
	}

	private synchronized ResourceMap getInputSources() throws IOException {
		File bundleFile = fetchBundle();

		_inputResources = new ResourceMap();
		try (JarInputStream in = new JarInputStream(IO.stream(bundleFile))) {
			JarEntry jarEntry = in.getNextJarEntry();
			while (jarEntry != null) {
				String entryPath = jarEntry.getName()
					.trim();
				if (entryPath.startsWith(dir)) {
					String relativePath = entryPath.substring(dir.length());
					if (!relativePath.isEmpty()) { // skip the root folder
						Resource resource;
						if (relativePath.endsWith("/")) {
							// strip the trailing slash
							relativePath.substring(0, relativePath.length());
							resource = new FolderResource();
						} else {
							// cannot use IO.collect() because it closes the
							// whole JarInputStream
							resource = BytesResource.loadFrom(in);
						}
						_inputResources.put(relativePath, resource);
					}

				}
				jarEntry = in.getNextJarEntry();
			}
		}
		return _inputResources;
	}

	private synchronized File fetchBundle() throws IOException {
		if (_bundleFile != null && _bundleFile.exists())
			return _bundleFile;

		Capability idCap = capability.getResource()
			.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)
			.get(0);
		String id = (String) idCap.getAttributes()
			.get(IdentityNamespace.IDENTITY_NAMESPACE);

		Capability contentCap = capability.getResource()
			.getCapabilities(ContentNamespace.CONTENT_NAMESPACE)
			.get(0);
		URI location;
		Object locationObj = contentCap.getAttributes()
			.get("url");
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

		// Try to locate from the workspace and/or repositories if a
		// BundleLocator was provide
		if (locator != null) {
			String hashStr = (String) contentCap.getAttributes()
				.get(ContentNamespace.CONTENT_NAMESPACE);
			try {
				_bundleFile = locator.locate(id, hashStr, "SHA-256", location);
				if (_bundleFile != null)
					return _bundleFile;
			} catch (Exception e) {
				throw new IOException("Unable to fetch bundle for template: " + getName(), e);
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

	@Override
	public void close() throws IOException {
		// nothing to do
	}

	private static void log(int level, String message, Throwable e) {
		Plugin.getDefault()
			.getLog()
			.log(new Status(level, Plugin.PLUGIN_ID, 0, message, e));
	}

	private static class FelixADAdapter implements AttributeDefinition {

		private final AD ad;

		public FelixADAdapter(AD ad) {
			this.ad = ad;
		}

		@Override
		public String getName() {
			return ad.getName();
		}

		@Override
		public String getID() {
			return ad.getID();
		}

		@Override
		public String getDescription() {
			return ad.getDescription();
		}

		@Override
		public int getCardinality() {
			return ad.getCardinality();
		}

		@Override
		public int getType() {
			return ad.getType();
		}

		@Override
		public String[] getOptionValues() {
			return ad.getOptionValues();
		}

		@Override
		public String[] getOptionLabels() {
			return ad.getOptionLabels();
		}

		@Override
		public String validate(String value) {
			return ad.validate(value);
		}

		@Override
		public String[] getDefaultValue() {
			return ad.getDefaultValue();
		}
	}

	private static class FelixOCDAdapter implements ObjectClassDefinition {

		private final OCD ocd;

		public FelixOCDAdapter(OCD ocd) {
			if (ocd == null)
				throw new NullPointerException();
			this.ocd = ocd;
		}

		@Override
		public String getName() {
			return ocd.getName();
		}

		@Override
		public String getID() {
			return ocd.getID();
		}

		@Override
		public String getDescription() {
			return ocd.getDescription();
		}

		@SuppressWarnings("unchecked")
		@Override
		public AttributeDefinition[] getAttributeDefinitions(int filter) {
			if (ocd.getAttributeDefinitions() == null)
				return null;

			Iterator<AD> iter = ocd.getAttributeDefinitions()
				.values()
				.iterator();
			if (filter == ObjectClassDefinition.OPTIONAL || filter == ObjectClassDefinition.REQUIRED) {
				boolean required = (filter == ObjectClassDefinition.REQUIRED);
				iter = new RequiredFilterIterator(iter, required);
			} else if (filter != ObjectClassDefinition.ALL) {
				return null;
			}

			if (!iter.hasNext())
				return null;

			List<AttributeDefinition> result = new ArrayList<>();
			while (iter.hasNext()) {
				result.add(new FelixADAdapter(iter.next()));
			}
			return result.toArray(new AttributeDefinition[0]);
		}

		@Override
		public InputStream getIcon(int size) throws IOException {
			// TODO
			return null;
		}

		@SuppressWarnings("rawtypes")
		private static class RequiredFilterIterator implements Iterator {

			private final Iterator	base;

			private final boolean	required;

			private AD				next;

			private RequiredFilterIterator(Iterator base, boolean required) {
				this.base = base;
				this.required = required;
				this.next = seek();
			}

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public Object next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}

				AD toReturn = next;
				next = seek();
				return toReturn;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove");
			}

			private AD seek() {
				if (base.hasNext()) {
					AD next;
					do {
						next = (AD) base.next();
					} while (next.isRequired() != required && base.hasNext());

					if (next.isRequired() == required) {
						return next;
					}
				}

				// nothing found any more
				return null;
			}

		}
	}

}
