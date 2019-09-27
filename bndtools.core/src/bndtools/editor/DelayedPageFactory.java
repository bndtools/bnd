package bndtools.editor;

import java.util.EnumSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.forms.editor.IFormPage;

import aQute.bnd.build.model.BndEditModel;

public class DelayedPageFactory implements IFormPageFactory {
	private static final ILogger		logger	= Logger.getLogger(DelayedPageFactory.class);
	private final IConfigurationElement	configElem;
	private final Set<Mode>				modes	= EnumSet.noneOf(Mode.class);

	public DelayedPageFactory(IConfigurationElement configElem) {
		this.configElem = configElem;

		String modeListStr = configElem.getAttribute("mode");
		if (modeListStr != null) {
			StringTokenizer tokenizer = new StringTokenizer(modeListStr, ",");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken()
					.trim();

				try {
					Mode mode = Enum.valueOf(Mode.class, token);
					if (mode != null)
						modes.add(mode);
				} catch (Exception e) {
					logger.logError("Invalid editor page mode: " + token, e);
				}
			}
		}
	}

	@Override
	public IFormPage createPage(ExtendedFormEditor editor, BndEditModel model, String id)
		throws IllegalArgumentException {
		try {
			IFormPageFactory factory = (IFormPageFactory) configElem.createExecutableExtension("class");
			return factory.createPage(editor, model, id);
		} catch (CoreException e) {
			logger.logError("Unable to create extension form page", e);
			throw new IllegalArgumentException("Error loading extension form page", e);
		}
	}

	@Override
	public boolean supportsMode(Mode mode) {
		return modes.contains(mode);
	}

}
