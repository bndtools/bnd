package name.neilbartlett.eclipse.bndtools.editor.project;

import java.util.List;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.VersionedClause;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.lib.osgi.Constants;

public class BuildPathPart extends RepositoryBundleSelectionPart {
	protected BuildPathPart(Composite parent, FormToolkit toolkit, int style) {
		super(Constants.BUILDPATH, parent, toolkit, style);
	}
	@Override
	protected void createSection(Section section, FormToolkit toolkit) {
		section.setText("Build Path");
		//section.setDescription("The selected bundles will be added to the project classpath for compilation.");
		super.createSection(section, toolkit);
	}
	@Override
	protected void saveToModel(BndEditModel model, List<VersionedClause> bundles) {
		model.setBuildPath(bundles);
	}
	@Override
	protected List<VersionedClause> loadFromModel(BndEditModel model) {
		return model.getBuildPath();
	}
}