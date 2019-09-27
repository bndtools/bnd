package bndtools.editor.project;

import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.resource.Requirement;

import bndtools.BndConstants;

public class RunBlacklistPart extends AbstractRequirementListPart {

	private static final String[] SUBSCRIBE_PROPS = new String[] {
		BndConstants.RUNBLACKLIST
	};

	public RunBlacklistPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	@Override
	protected String[] getProperties() {
		return SUBSCRIBE_PROPS;
	}

	private void createSection(Section section, FormToolkit tk) {
		section.setText("Run Blacklist");
		section.setDescription("The specified requirements will be excluded from the resolution.");

		// Create toolbar
		createToolBar(section);

		// Create main panel
		Composite composite = tk.createComposite(section);
		section.setClient(composite);

		// Create table
		TableViewer viewer = createViewer(composite, tk);

		// Layout
		GridLayout layout;
		GridData gd;

		layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 5;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.widthHint = 50;
		gd.heightHint = 50;
		viewer.getControl()
			.setLayoutData(gd);

		gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
	}

	@Override
	protected void doCommitToModel(List<Requirement> requires) {
		model.setRunBlacklist(requires);
	}

	@Override
	protected List<Requirement> doRefreshFromModel() {
		return model.getRunBlacklist();
	}

	@Override
	protected String getAddButtonLabel() {
		return "Add to Blacklist";
	}

}
