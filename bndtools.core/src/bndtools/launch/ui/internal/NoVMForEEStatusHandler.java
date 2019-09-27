package bndtools.launch.ui.internal;

import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class NoVMForEEStatusHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		if (status.isOK())
			return true;

		Display display = PlatformUI.getWorkbench()
			.getDisplay();
		SWTConcurrencyUtil.execForDisplay(display, true, () -> {
			new NoVMForEEStatusDialog(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getShell(), status.getMessage()).open();
		});
		return false;
	}

}

class NoVMForEEStatusDialog extends TitleAreaDialog {

	private final String message;

	public NoVMForEEStatusDialog(Shell parentShell, String message) {
		super(parentShell);
		this.message = message;
		setShellStyle(SWT.BORDER | SWT.RESIZE | SWT.TITLE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Unmatched Execution Environment");

		setErrorMessage("Could not find a suitable JRE installation for launch.");

		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, true));

		Link link = new Link(container, SWT.WRAP);
		link.setText(String.format(
			"%s\n\nReview the <a href=\"#ee\">Execution Environment preferences</a> and/or the <a href=\"#jre\">Installed JRE preferences</a>.",
			message));

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// NoVMForEEStatusDialog.this.close();
				if ("#ee".equals(event.text)) {
					PreferencesUtil
						.createPreferenceDialogOn(getParentShell(), "org.eclipse.jdt.debug.ui.jreProfiles", null, null)
						.open();
				} else if ("#jre".equals(event.text)) {
					PreferencesUtil
						.createPreferenceDialogOn(getParentShell(),
							"org.eclipse.jdt.debug.ui.preferences.VMPreferencePage", null, null)
						.open();
				}
			}
		});

		return container;
	}

}
