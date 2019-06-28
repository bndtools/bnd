package bndtools.editor.workspace;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.model.clauses.HeaderClause;

public class PluginSelectionWizard extends Wizard {

	private final PluginClassSelectionPage	classPage		= new PluginClassSelectionPage();
	private final PluginPropertiesPage		propertiesPage	= new PluginPropertiesPage();

	private HeaderClause					header;

	public PluginSelectionWizard() {
		addPage(classPage);
		addPage(propertiesPage);

		classPage.addPropertyChangeListener("selectedElement",
			evt -> propertiesPage.setConfigElement((IConfigurationElement) evt.getNewValue()));
	}

	@Override
	public boolean performFinish() {
		header = new HeaderClause(classPage.getSelectedElement()
			.getAttribute("class"), propertiesPage.getProperties());
		return true;
	}

	public HeaderClause getHeader() {
		return header;
	}
}
