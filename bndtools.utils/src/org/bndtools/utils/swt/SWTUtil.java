package org.bndtools.utils.swt;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class SWTUtil {

	public static final String OVERRIDE_ENABLEMENT = "override.enable";

	public interface OverrideEnablement {
		boolean override(boolean outerEnable);
	}

	public static void recurseEnable(boolean enable, Control control) {
		Object data = control.getData();
		boolean en = enable;
		if (data != null && data instanceof OverrideEnablement)
			en = ((OverrideEnablement) data).override(en);
		else {
			data = control.getData(OVERRIDE_ENABLEMENT);
			if (data != null && data instanceof OverrideEnablement)
				en = ((OverrideEnablement) data).override(en);
		}
		control.setEnabled(en);
		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				recurseEnable(en, child);
			}
		} else if (control instanceof Label) {
			Color color = enable ? control.getDisplay()
				.getSystemColor(SWT.COLOR_BLACK)
				: JFaceResources.getColorRegistry()
					.get(JFacePreferences.QUALIFIER_COLOR);
			control.setForeground(color);
		}
	}

	public static void setHorizontalGrabbing(Control control) {
		Object ld = control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData) ld).grabExcessHorizontalSpace = true;
		}
	}

	// Shamelessly stolen from JDT
	public static int getButtonWidthHint(Button button) {
		PixelConverter converter = new PixelConverter(button);
		int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}

}
