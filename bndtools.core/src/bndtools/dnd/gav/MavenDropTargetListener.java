package bndtools.dnd.gav;

import static bndtools.dnd.gav.MavenDropTargetListener.Syntax.MAVEN;
import static bndtools.dnd.gav.MavenDropTargetListener.Syntax.MAVEN_NO_VERSION;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.custom.StyledText;
import org.osgi.resource.Resource;

import aQute.bnd.maven.MavenCapability;
import aQute.bnd.service.RepositoryPlugin;

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
	String format(Resource resource, RepositoryPlugin repositoryPlugin, boolean noVersion, boolean useAlternateSyntax,
		String indentPrefix) {
		if (noVersion) {
			return format(resource, MAVEN_NO_VERSION, indentPrefix, indent(isTabs(), getSize()));
		}
		return format(resource, MAVEN, indentPrefix, indent(isTabs(), getSize()));
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

	@SuppressWarnings("null")
	String format(Resource resource, Syntax syntax, String indentPrefix, String indent) {
		MavenCapability mc = MavenCapability.getMavenCapability(resource);

		if (mc == null) {
			return "No maven information was indexed for resource " + resource;
		}

		String group = mc.maven_groupId();
		String identity = mc.maven_artifactId();
		String version = mc
			.maven_version()
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
				sb.append(
					indentPrefix)
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

		return sb.toString();
	}

}
