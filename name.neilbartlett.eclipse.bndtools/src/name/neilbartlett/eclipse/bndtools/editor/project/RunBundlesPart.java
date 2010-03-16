package name.neilbartlett.eclipse.bndtools.editor.project;

import java.util.List;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.VersionedClause;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.lib.osgi.Constants;

public class RunBundlesPart extends RepositoryBundleSelectionPart {
	protected RunBundlesPart(Composite parent, FormToolkit toolkit, int style) {
		super(Constants.RUNBUNDLES, parent, toolkit, style);
	}
	@Override
	protected void createSection(Section section, FormToolkit toolkit) {
		section.setText("Run Bundles");
//		section.setDescription("The selected bundles will be added to the runtime framework.");
		super.createSection(section, toolkit);
	}
	@Override
	protected void saveToModel(BndEditModel model, List<VersionedClause> bundles) {
		model.setRunBundles(bundles);
	}
	@Override
	protected List<VersionedClause> loadFromModel(BndEditModel model) {
		return model.getRunBundles();
	}
}
