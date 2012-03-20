package org.bndtools.core.utils.swt;

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
        if (data != null && data instanceof OverrideEnablement)
            enable = ((OverrideEnablement) data).override(enable);
        else {
            data = control.getData(OVERRIDE_ENABLEMENT);
            if (data != null && data instanceof OverrideEnablement)
                enable = ((OverrideEnablement) data).override(enable);
        }
        control.setEnabled(enable);
        if (control instanceof Composite) {
            for (Control child : ((Composite) control).getChildren()) {
                recurseEnable(enable, child);
            }
        }
    }

    public static void setHorizontalGrabbing(Control control) {
        Object ld= control.getLayoutData();
        if (ld instanceof GridData) {
            ((GridData)ld).grabExcessHorizontalSpace= true;
        }
    }

}
