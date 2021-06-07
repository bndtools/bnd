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
	void format(FormatEvent formatEvent) {
		if (formatEvent.isNoVersion()) {
			Syntax syntax = Syntax.BND_VERSION_LATEST;
			if (formatEvent.getRepositoryPlugin() instanceof WorkspaceRepository) {
				syntax = Syntax.BND_VERSION_SNAPSHOT;
			}
			format(formatEvent.getResource(), formatEvent.useAlternateSyntax() ? Syntax.BND_NO_VERSION : syntax,
				formatEvent.getLineAtInsertionPoint(), formatEvent.getIndentPrefix(), indent(isTabs(), getSize()));
		} else {
			format(formatEvent.getResource(), Syntax.BND, formatEvent.getLineAtInsertionPoint(),
				formatEvent.getIndentPrefix(), indent(isTabs(), getSize()));
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

	private void format(Resource resource, Syntax syntax, String lineAtInsertionPoint, String indentPrefix,
		String indent) {
		IdentityCapability ic = ResourceUtils.getIdentityCapability(resource);
		InfoCapability info = BridgeRepository.getInfo(resource);

		String identity = "";
		String version = "";

		if (ic == null) {
			if (info == null) {
				// we shouldn't be able to get here!
				return;
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

		if (lineAtInsertionPoint.trim()
			.startsWith("-")) {
			indentPrefix += indent;
		}

		boolean lineHasContinuation = lineAtInsertionPoint.endsWith("\\");

		StringBuilder sb = new StringBuilder();

		if (!lineHasContinuation) {
			sb.append(",\\");
		}

		sb.append("\n")
			.append(indentPrefix.isEmpty() ? indent : indentPrefix)
			.append(identity);

		switch (syntax) {
			case BND :
				sb.append(";version='")
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

		getStyledText().insert(sb.toString());
	}

}
