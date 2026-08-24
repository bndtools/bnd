package bndtools.editor.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;

/** Quickfixes for {@link IncludeConflictDetector#MARKER_TYPE}: rename the plain key in one of the conflicting files to merged syntax. */
public class IncludeConflictMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	private static final ILogger logger = Logger.getLogger(IncludeConflictMarkerResolutionGenerator.class);

	@Override
	public boolean hasResolutions(IMarker marker) {
		return marker.getAttribute(IncludeConflictDetector.ATTR_KEY, null) != null;
	}

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		String key = marker.getAttribute(IncludeConflictDetector.ATTR_KEY, null);
		String sources = marker.getAttribute(IncludeConflictDetector.ATTR_SOURCES, "");
		if (key == null || sources.isEmpty())
			return new IMarkerResolution[0];

		if (marker.getAttribute(IncludeConflictDetector.ATTR_IN_FILE, false)) {
			File file = new File(sources);
			if (!file.isFile())
				return new IMarkerResolution[0];
			return new IMarkerResolution[] {
				new IMarkerResolution2() {
					@Override
					public String getLabel() {
						return "Rename duplicate occurrences of '" + key + "' in " + file.getName()
							+ " to merged syntax";
					}

					@Override
					public String getDescription() {
						return "Keeps the first definition and renames later occurrences in " + file.getAbsolutePath()
							+ " to unique '" + key + ".<suffix>' keys so bnd merges them instead of overwriting.";
					}

					@Override
					public Image getImage() {
						return null;
					}

					@Override
					public void run(IMarker m) {
						try {
							IncludeConflictDetector.renameDuplicateKeysInFile(file, key);
							m.delete();
						} catch (Exception e) {
							logger.logError("Failed to rename duplicates of " + key + " in " + file, e);
						}
					}
				}
			};
		}

		List<IMarkerResolution> resolutions = new ArrayList<>();
		for (String path : sources.split(";")) {
			File file = new File(path);
			if (!file.isFile())
				continue;
			String newKey = key + "." + IncludeConflictDetector.suggestSuffixForFile(file, key);
			resolutions.add(new IMarkerResolution2() {
				@Override
				public String getLabel() {
					return "Rename '" + key + "' to '" + newKey + "' in " + file.getName();
				}

				@Override
				public String getDescription() {
					return "Renames the plain property in " + file.getAbsolutePath()
						+ " so bnd merges it with the other definitions instead of shadowing them.";
				}

				@Override
				public Image getImage() {
					return null;
				}

				@Override
				public void run(IMarker m) {
					try {
						IncludeConflictDetector.renameKeyInFile(file, key, newKey);
						m.delete();
					} catch (Exception e) {
						logger.logError("Failed to rename " + key + " in " + file, e);
					}
				}
			});
		}
		return resolutions.toArray(new IMarkerResolution[0]);
	}
}
