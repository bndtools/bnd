package bndtools.preferences.ui;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import bndtools.preferences.JpmPreferences;

public class JpmPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private JpmPreferences prefs;
    private int browserSelection;

    @Override
    public void init(IWorkbench workbench) {
        prefs = new JpmPreferences();
        browserSelection = prefs.getBrowserSelection();
    }

    @Override
    public boolean performOk() {
        prefs.setBrowserSelection(browserSelection);
        return true;
    }

    @Override
    protected Control createContents(Composite parent) {
        // Create controls
        Composite composite = new Composite(parent, SWT.NONE);

        Group grpBrowser = new Group(composite, SWT.NONE);
        grpBrowser.setText("Browser Selection");

        final Button[] btnsBrowser = new Button[JpmPreferences.PREF_BROWSER_SELECTION_CHOICES.length];
        for (int i = 0; i < btnsBrowser.length; i++) {
            btnsBrowser[i] = new Button(grpBrowser, SWT.RADIO);
            btnsBrowser[i].setText(JpmPreferences.PREF_BROWSER_SELECTION_CHOICES[i]);
            btnsBrowser[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        }

        // Layout
        composite.setLayout(new GridLayout(1, false));
        grpBrowser.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        grpBrowser.setLayout(new GridLayout(1, false));

        // Load data
        for (int i = 0; i < btnsBrowser.length; i++) {
            btnsBrowser[i].setSelection(i == browserSelection);
        }

        // Listeners
        SelectionAdapter adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selected = 0;
                for (int i = 0; i < btnsBrowser.length; i++) {
                    if (btnsBrowser[i].getSelection()) {
                        selected = i;
                        break;
                    }
                }
                browserSelection = selected;
                updateMessages();
            }
        };
        for (int i = 0; i < btnsBrowser.length; i++) {
            btnsBrowser[i].addSelectionListener(adapter);
        }

        return composite;
    }

    private void updateMessages() {
        String warning = null;
        if (browserSelection != prefs.getBrowserSelection())
            warning = "JPM view must be closed and reopened";
        setMessage(warning, IMessageProvider.WARNING);
    }

}
