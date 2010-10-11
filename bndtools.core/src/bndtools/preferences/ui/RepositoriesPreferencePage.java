package bndtools.preferences.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import bndtools.Plugin;

public class RepositoriesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        return composite;
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return Plugin.getDefault().getPreferenceStore();
    }

}
