package bndtools.editor.workspace;

import java.util.function.Supplier;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.model.BndEditModelHeaderClause;
import aQute.bnd.build.model.clauses.HeaderClause;

public class PluginSelectionWizard extends Wizard {

	private final PluginClassSelectionPage	classPage		= new PluginClassSelectionPage();
	private final PluginPropertiesPage		propertiesPage	= new PluginPropertiesPage();

	private BndEditModelHeaderClause		header;
	private Supplier<String>				uniqueKeySupplier;

	public PluginSelectionWizard(Supplier<String> uniqueKeySupplier) {
		addPage(classPage);
		addPage(propertiesPage);

		this.uniqueKeySupplier = uniqueKeySupplier;
		classPage.addPropertyChangeListener("selectedElement",
			evt -> propertiesPage.setConfigElement((IConfigurationElement) evt.getNewValue()));
	}

	@Override
	public boolean performFinish() {
		header = new BndEditModelHeaderClause(uniqueKeySupplier.get(), new HeaderClause(classPage.getSelectedElement()
			.getAttribute("class"), propertiesPage.getProperties()), true);
		return true;
	}

	public BndEditModelHeaderClause getHeader() {
		return header;
	}
}
