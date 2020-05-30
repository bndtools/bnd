package bndtools.dnd.gav;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.custom.StyledText;
import org.osgi.resource.Resource;

import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.BridgeRepository.InfoCapability;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.service.RepositoryPlugin;

public class BndDropTargetListener extends GAVDropTargetListener {

	enum Syntax {
		BND,
		BND_NO_VERSION,
		BND_VERSION_LATEST,
		BND_VERSION_SNAPSHOT
	}

	private final static IPreferencesService prefsService = Platform.getPreferencesService();

	public BndDropTargetListener(StyledText styledText) {
		super(styledText);
	}

	@Override
	String format(Resource resource, RepositoryPlugin repositoryPlugin, boolean noVersion, boolean useAlternateSyntax,
		String lineAtInsertionPoint, String indentPrefix) {
		if (noVersion) {
			Syntax syntax = Syntax.BND_VERSION_LATEST;
			if (repositoryPlugin instanceof WorkspaceRepository) {
				syntax = Syntax.BND_VERSION_SNAPSHOT;
			}
			return format(resource, useAlternateSyntax ? Syntax.BND_NO_VERSION : syntax, lineAtInsertionPoint,
				indentPrefix,
				indent(isTabs(), getSize()));
		}
		return format(resource, Syntax.BND, lineAtInsertionPoint, indentPrefix, indent(isTabs(), getSize()));
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
		return prefsService.getBoolean("org.eclipse.ui.editors.prefs", "spacesForTabs", false, null);
	}

	private String format(Resource resource, Syntax syntax, String lineAtInsertionPoint, String indentPrefix,
		String indent) {
		IdentityCapability ic = ResourceUtils.getIdentityCapability(resource);
		InfoCapability info = BridgeRepository.getInfo(resource);

		String identity = "";
		String version = "";

		if (ic == null) {
			if (info == null) {
				// we shouldn't be able to get here!
				return "No information was indexed for resource " + resource;
			} else {
				identity = info.name();
				version = info.version()
					.toString();
			}
		} else {
			identity = ic.osgi_identity();
			version = ic.version()
				.toString();
		}

		boolean lineHasContinuation = lineAtInsertionPoint.endsWith("\\");

		StringBuilder sb = new StringBuilder();

		if (!lineHasContinuation) {
			sb.append(",\\");
		}

		sb.append(
			"\n")
			.append(indentPrefix.isEmpty() ? indent
				: indentPrefix)
			.append(identity);

		switch (syntax) {
			case BND :
				sb.append(
					";version='")
					.append(version)
					.append("'");
				break;
			case BND_NO_VERSION :
				break;
			case BND_VERSION_LATEST :
				sb.append(";version=latest");
				break;
			case BND_VERSION_SNAPSHOT :
				sb.append(";version=snapshot");
				break;
		}
		if (lineHasContinuation) {
			sb.append(",\\");
		}

		return sb.toString();
	}

}
