package bndtools.dnd.gav;

import static bndtools.dnd.gav.GradleDropTargetListener.Syntax.GRADLE_MAP;
import static bndtools.dnd.gav.GradleDropTargetListener.Syntax.GRADLE_MAP_NO_VERSION;
import static bndtools.dnd.gav.GradleDropTargetListener.Syntax.GRADLE_STRING;
import static bndtools.dnd.gav.GradleDropTargetListener.Syntax.GRADLE_STRING_NO_VERSION;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.custom.StyledText;
import org.osgi.resource.Resource;

import aQute.bnd.maven.MavenCapability;

public class GradleDropTargetListener extends GAVDropTargetListener {

	enum Syntax {
		GRADLE_MAP,
		GRADLE_MAP_NO_VERSION,
		GRADLE_STRING,
		GRADLE_STRING_NO_VERSION
	}

	private final static IPreferencesService	prefsService	= Platform.getPreferencesService();

	public GradleDropTargetListener(StyledText styledText) {
		super(styledText);
	}

	@Override
	void format(FormatEvent formatEvent) {
		if (formatEvent.isNoVersion()) {
			format(formatEvent
				.getResource(),
				formatEvent.useAlternateSyntax() ? GRADLE_MAP_NO_VERSION : GRADLE_STRING_NO_VERSION,
				formatEvent.getLineAtInsertionPoint(),
				formatEvent
					.getIndentPrefix(),
				indent(isTabs(), getSize()));
		}
		else {
			format(formatEvent.getResource(), formatEvent.useAlternateSyntax() ? GRADLE_MAP : GRADLE_STRING,
				formatEvent.getLineAtInsertionPoint(), formatEvent.getIndentPrefix(), indent(isTabs(), getSize()));
		}
	}

	@Override
	boolean hasAlternateSyntax() {
		return true;
	}

	int getSize() {
		if (isTabs()) {
			return 1;
		}
		return prefsService.getInt("org.eclipse.ui.editors.prefs", "tabWidth", 4, null);
	}

	boolean isTabs() {
		return !prefsService.getBoolean("org.eclipse.ui.editors.prefs", "spacesForTabs", false, null);
	}

	private void format(Resource resource, Syntax syntax, String lineAtInsertionPoint,
		String indentPrefix,
		String indent) {
		MavenCapability mc = MavenCapability.getMavenCapability(resource);

		if (mc == null) {
			return;
		}

		if (lineAtInsertionPoint.trim()
			.startsWith("dependencies")) {
			indentPrefix += indent;
		}

		String group = mc.maven_groupId();
		String identity = mc.maven_artifactId();
		String version = mc.maven_version()
			.toString();
		String classifier = mc.maven_classifier();

		StringBuilder sb = new StringBuilder();
		sb.append("\n")
			.append(indentPrefix)
			.append("implementation ");

		switch (syntax) {
			case GRADLE_MAP :
				sb.append(
					"group: \"")
					.append(group)
					.append("\", name: \"")
					.append(identity)
					.append("\", version: \"")
					.append(version)
					.append("\"");
				if (!classifier.isEmpty()) {
					sb.append(", classifier: \"")
						.append(classifier)
						.append("\"");
				}
				break;
			case GRADLE_MAP_NO_VERSION :
				sb.append(
					"group: \"")
					.append(group)
					.append("\", name: \"")
					.append(identity)
					.append("\"");
				if (!classifier.isEmpty()) {
					sb.append(", classifier: \"")
						.append(classifier)
						.append("\"");
				}
				break;
			case GRADLE_STRING :
				sb.append(
					"'")
					.append(group)
					.append(":")
					.append(identity)
					.append(":")
					.append(version);
				if (!classifier.isEmpty()) {
					sb.append(":")
						.append(classifier);
				}
				sb.append("'");
				break;
			case GRADLE_STRING_NO_VERSION :
				sb.append(
					"'")
					.append(group)
					.append(":")
					.append(identity)
					// You cannot specify a classifier without specifying
					// version in this form
					.append("'");
				break;
		}

		getStyledText().insert(sb.toString());
	}

}
