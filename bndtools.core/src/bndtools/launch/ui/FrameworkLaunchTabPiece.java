package bndtools.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import bndtools.launch.LaunchConstants;

public class FrameworkLaunchTabPiece extends AbstractLaunchTabPiece {

	private boolean	dynamicUpdate	= true;
	private boolean	clean			= false;

	private Button	dynamicUpdateBtn;
	private Button	cleanBtn;

	@Override
	public Control createControl(Composite parent) {
		Group frameworkGroup = new Group(parent, SWT.NONE);
		frameworkGroup.setText("Framework:");

		dynamicUpdateBtn = new Button(frameworkGroup, SWT.CHECK);
		dynamicUpdateBtn.setText("Update bundles during runtime.");

		cleanBtn = new Button(frameworkGroup, SWT.CHECK);
		cleanBtn.setText("Clean storage area before launch.");

		// LISTENERS
		dynamicUpdateBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
				boolean oldDynamic = dynamicUpdate;
				dynamicUpdate = dynamicUpdateBtn.getSelection();

				firePropertyChange("dynamicUpdate", oldDynamic, dynamicUpdate);
			}
		});
		cleanBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
				boolean oldClean = clean;
				clean = cleanBtn.getSelection();

				firePropertyChange("clean", oldClean, clean);
			}
		});

		// LAYOUT
		GridLayout layout = new GridLayout(1, false);
		frameworkGroup.setLayout(layout);

		dynamicUpdateBtn.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		cleanBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return frameworkGroup;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		dynamicUpdate = configuration.getAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES,
			LaunchConstants.DEFAULT_DYNAMIC_BUNDLES);
		dynamicUpdateBtn.setSelection(dynamicUpdate);

		clean = configuration.getAttribute(LaunchConstants.ATTR_CLEAN, LaunchConstants.DEFAULT_CLEAN);
		cleanBtn.setSelection(clean);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, dynamicUpdate);
		configuration.setAttribute(LaunchConstants.ATTR_CLEAN, clean);
	}
}
