package bndtools.editor.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.PropertyConflict;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.service.reporter.Report.Location;

/** Presents core property-conflict diagnostics and applies editor-only repairs. */
public class IncludeConflictDetector {
	private static final ILogger logger = Logger.getLogger(IncludeConflictDetector.class);
	public static final String MARKER_TYPE = "bndtools.core.includeconflict";
	public static final String ATTR_KEY = "conflictKey";
	public static final String ATTR_SOURCES = "conflictSources";
	public static final String ATTR_IN_FILE = "conflictInFile";
	public static final String ATTR_MERGEABLE = "conflictMergeable";
	public static final String ATTR_ROOT = "conflictRoot";
	private static final String ATTR_OWNER = "conflictEditorOwner";

	/** Validates the current document through core, without modifying the shared owner. */
	public static void updateMarkers(IResource resource, BndEditModel model, String content) {
		if (resource == null || !resource.exists() || model == null || model.getBndResource() == null)
			return;
		Processor parent = model.getOwner();
		if (parent != null && model.getBndResource().getAbsoluteFile().equals(parent.getPropertiesFile()))
			parent = parent.getParent();
		try (Processor snapshot = PropertyConflict.analyze(parent, model.getBndResource(), content)) {
			String owner = resource.getFullPath().toString();
			resource.getWorkspace().run(monitor -> {
				for (IMarker marker : resource.getWorkspace().getRoot().findMarkers(MARKER_TYPE, false, IResource.DEPTH_INFINITE)) {
					if (owner.equals(marker.getAttribute(ATTR_OWNER, ""))
						|| (marker.getResource().equals(resource) && marker.getAttribute(ATTR_OWNER, "").isEmpty()))
						marker.delete();
				}
				createMarkers(resource, snapshot, snapshot.getErrors(), IMarker.SEVERITY_ERROR, owner);
				createMarkers(resource, snapshot, snapshot.getWarnings(), IMarker.SEVERITY_WARNING, owner);
			}, resource.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE, null);
		} catch (Exception exception) {
			logger.logError("Unable to update property-conflict markers", exception);
		}
	}

	private static void createMarkers(IResource fallback, Processor processor, List<String> messages, int severity,
		String owner) throws org.eclipse.core.runtime.CoreException {
		for (String message : messages) {
			Location location = processor.getLocation(message);
			if (location == null || !(location.details instanceof PropertyConflict conflict))
				continue;
			IResource target = target(location, fallback);
			IMarker marker = target.createMarker(MARKER_TYPE);
			marker.setAttributes(attributes(conflict, location, processor));
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(ATTR_OWNER, owner);
		}
	}

	static IResource target(Location location, IResource fallback) {
		if (location.file != null && new File(location.file).isAbsolute()) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(location.file));
			if (file != null && file.exists())
				return file;
		}
		return fallback;
	}

	static Map<String, Object> attributes(PropertyConflict conflict, Location location, Processor processor) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(IMarker.MESSAGE, location.message);
		attributes.put(IMarker.LINE_NUMBER, location.line + 1);
		attributes.put(ATTR_KEY, conflict.key());
		attributes.put(ATTR_IN_FILE, conflict.kind() == PropertyConflict.Kind.DUPLICATE);
		attributes.put(ATTR_MERGEABLE, conflict.mergeable());
		attributes.put(ATTR_SOURCES, conflict.occurrences().stream().map(PropertyConflict.Occurrence::source)
			.distinct().collect(Collectors.joining("\n")));
		if (processor.getPropertiesFile() != null)
			attributes.put(ATTR_ROOT, processor.getPropertiesFile().getAbsolutePath());
		return attributes;
	}

	static void renameKeysInFile(File file, String key, File root, boolean duplicates) throws Exception {
		if (!PropertyConflict.isMergedHeader(key))
			throw new IllegalArgumentException("Property does not support merged syntax: " + key);
		IFile resource = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
		if (resource == null || !resource.exists())
			throw new IllegalArgumentException("Property source is outside the workspace: " + file);
		var manager = FileBuffers.getTextFileBufferManager();
		var path = resource.getFullPath();
		manager.connect(path, LocationKind.IFILE, null);
		try {
			ITextFileBuffer buffer = manager.getTextFileBuffer(path, LocationKind.IFILE);
			boolean dirty = buffer.isDirty();
			IDocument document = buffer.getDocument();
			Set<String> used = new HashSet<>();
			try (Processor processor = new Processor()) {
				processor.setProperties(root);
				used.addAll(processor.getProperties().stringPropertyNames());
				List<File> files = new ArrayList<>(processor.getIncluded());
				files.add(root);
				for (File included : files) {
					IFile includedResource = ResourcesPlugin.getWorkspace().getRoot()
						.getFileForLocation(new Path(included.getAbsolutePath()));
					if (includedResource == null)
						continue;
					ITextFileBuffer open = manager.getTextFileBuffer(includedResource.getFullPath(), LocationKind.IFILE);
					if (open != null) {
						UTF8Properties properties = new UTF8Properties();
						properties.load(open.getDocument().get(), included, null);
						used.addAll(properties.stringPropertyNames());
					}
				}
			}
			document.set(renameKeys(document.get(), key, suffixBase(file), used, duplicates));
			if (!dirty)
				buffer.commit(null, false);
		} finally {
			manager.disconnect(path, LocationKind.IFILE, null);
		}
	}

	static String renameKeys(String content, String key, String base, Set<String> used, boolean duplicates) throws Exception {
		if (!PropertyConflict.isMergedHeader(key))
			throw new IllegalArgumentException("Property does not support merged syntax: " + key);
		List<UTF8Properties.Property> declarations = new ArrayList<>();
		UTF8Properties properties = new UTF8Properties();
		properties.load(content, null, null, null, "", declarations::add);
		Set<String> occupied = new HashSet<>(used);
		occupied.addAll(properties.stringPropertyNames());
		List<UTF8Properties.Property> occurrences = declarations.stream().filter(property -> property.key().equals(key)).toList();
		List<String> replacements = new ArrayList<>();
		int first = duplicates ? 1 : 0;
		int limit = duplicates ? occurrences.size() : Math.min(1, occurrences.size());
		for (int index = first; index < limit; index++) {
			String replacement = key + "." + base;
			int suffix = 2;
			while (!occupied.add(replacement))
				replacement = key + "." + base + "-" + suffix++;
			replacements.add(replacement);
		}
		StringBuilder result = new StringBuilder(content);
		for (int index = limit - 1; index >= first; index--) {
			UTF8Properties.Property occurrence = occurrences.get(index);
			result.replace(occurrence.start(), occurrence.end(), replacements.get(index - first));
		}
		return result.toString();
	}

	private static String suffixBase(File file) {
		String name = file.getName().replaceFirst("\\.bnd(?:run)?$", "").replaceAll("[^A-Za-z0-9._-]", "-");
		return name.isEmpty() ? "local" : name;
	}

	private IncludeConflictDetector() {}
}
