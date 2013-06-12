package org.bndtools.utils.swt;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class SWTUtil {

    public static final String OVERRIDE_ENABLEMENT = "override.enable";

    public static interface OverrideEnablement {
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
        }
    }

    public static void setHorizontalGrabbing(Control control) {
        Object ld = control.getLayoutData();
        if (ld instanceof GridData) {
            ((GridData) ld).grabExcessHorizontalSpace = true;
        }
    }

}
