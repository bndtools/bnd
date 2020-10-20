package bndtools.dnd.gav;

import static bndtools.dnd.gav.MavenDropTargetListener.Syntax.MAVEN;
import static bndtools.dnd.gav.MavenDropTargetListener.Syntax.MAVEN_NO_VERSION;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.custom.StyledText;
import org.osgi.resource.Resource;

import aQute.bnd.maven.MavenCapability;

public class MavenDropTargetListener extends GAVDropTargetListener {

	enum Syntax {
		MAVEN,
		MAVEN_NO_VERSION
	}

	private final static IPreferencesService prefsService = Platform.getPreferencesService();

	public MavenDropTargetListener(StyledText styledText) {
		super(styledText);
	}

	@Override
	void format(FormatEvent formatEvent) {
		if (formatEvent.isNoVersion()) {
			format(formatEvent.getResource(), MAVEN_NO_VERSION, formatEvent.getLineAtInsertionPoint(),
				formatEvent.getIndentPrefix(), indent(isTabs(), getSize()));
		} else {
			format(formatEvent.getResource(), MAVEN, formatEvent.getLineAtInsertionPoint(),
				formatEvent.getIndentPrefix(), indent(isTabs(), getSize()));
		}
	}

	@Override
	boolean hasAlternateSyntax() {
		return false;
	}

	int getSize() {
		if (isTabs()) {
			return 1;
		}
		return prefsService.getInt("org.eclipse.wst.xml.core.prefs", "indentationSize", 4, null);
	}

	boolean isTabs() {
		return prefsService.getString("org.eclipse.wst.xml.core.prefs", "indentationChar", "tab", null)
			.equals("tab");
	}

	private void format(Resource resource, Syntax syntax, String lineAtInsertionPoint, String indentPrefix,
		String indent) {
		MavenCapability mc = MavenCapability.getMavenCapability(resource);

		if (mc == null) {
			return;
		}

		if (lineAtInsertionPoint.trim()
			.startsWith("<dependencies>")) {
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
			.append("<dependency>\n")
			.append(indentPrefix)
			.append(indent)
			.append("<groupId>")
			.append(group)
			.append("</groupId>\n")
			.append(indentPrefix)
			.append(indent)
			.append("<artifactId>")
			.append(identity)
			.append("</artifactId>\n");

		switch (syntax) {
			case MAVEN :
				sb.append(indentPrefix)
					.append(indent)
					.append("<version>")
					.append(version)
					.append("</version>\n");
				break;
			case MAVEN_NO_VERSION :
				break;
		}

		sb.append(indentPrefix)
			.append("</dependency>");

		getStyledText().insert(sb.toString());
	}

}
