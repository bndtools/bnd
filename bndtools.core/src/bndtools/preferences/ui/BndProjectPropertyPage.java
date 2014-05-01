package bndtools.preferences.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import bndtools.Plugin;
import bndtools.preferences.CompileErrorAction;
import bndtools.preferences.EclipseClasspathPreference;

public class BndProjectPropertyPage extends PropertyPage {

    private CompileErrorAction action;
    private EclipseClasspathPreference classpathPref;

    public BndProjectPropertyPage() {
        setTitle("Bndtools");
    }

    /**
     * Create contents of the property page.
     * 
     * @param parent
     */
    @Override
    public Control createContents(Composite parent) {
        // CREATE CONTROLS
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(1, false));

        Group grpJavaCompilationErrors = new Group(container, SWT.NONE);
        grpJavaCompilationErrors.setLayout(new GridLayout(1, false));
        grpJavaCompilationErrors.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        grpJavaCompilationErrors.setText("Java Compilation Errors");

        Label lblHowShouldBndtools = new Label(grpJavaCompilationErrors, SWT.WRAP);
        lblHowShouldBndtools.setBounds(0, 0, 59, 14);
        lblHowShouldBndtools.setText("How should Bndtools proceed when Java compilation errors exist?");

        final Button btnDelete = new Button(grpJavaCompilationErrors, SWT.RADIO);
        btnDelete.setText("Delete the output bundle");

        final Button btnSkip = new Button(grpJavaCompilationErrors, SWT.RADIO);
        btnSkip.setText("Skip building the output bundle (default)");

        final Button btnContinue = new Button(grpJavaCompilationErrors, SWT.RADIO);
        btnContinue.setText("Continue building the bundle");

        Group grpEclipseClasspathEntries = new Group(container, SWT.NONE);
        grpEclipseClasspathEntries.setLayout(new GridLayout(1, false));
        grpEclipseClasspathEntries.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        grpEclipseClasspathEntries.setText("Eclipse Classpath Entries");

        final Button btnExposeEclipseClasspath = new Button(grpEclipseClasspathEntries, SWT.RADIO);
        btnExposeEclipseClasspath.setText("Expose Eclipse classpath entries to bnd");

        final Button btnHideEclipseClasspath = new Button(grpEclipseClasspathEntries, SWT.RADIO);
        btnHideEclipseClasspath.setText("Hide Eclipse classpath entries (safer)");

        // LOAD DATA
        IPreferenceStore store = getPreferenceStore();
        action = CompileErrorAction.parse(store.getString(CompileErrorAction.PREFERENCE_KEY));
        switch (action) {
        case delete :
            btnDelete.setSelection(true);
            btnSkip.setSelection(false);
            btnContinue.setSelection(false);
            break;
        case skip :
        default :
            btnDelete.setSelection(false);
            btnSkip.setSelection(true);
            btnContinue.setSelection(false);
            break;
        case build :
            btnDelete.setSelection(false);
            btnSkip.setSelection(false);
            btnContinue.setSelection(true);
            break;
        }
        classpathPref = EclipseClasspathPreference.parse(store.getString(EclipseClasspathPreference.PREFERENCE_KEY));
        switch (classpathPref) {
        case expose :
        default :
            btnExposeEclipseClasspath.setSelection(true);
            btnHideEclipseClasspath.setSelection(false);
            break;
        case hide :
            btnExposeEclipseClasspath.setSelection(false);
            btnHideEclipseClasspath.setSelection(true);
        }

        // LISTENERS
        SelectionAdapter errorActionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (btnDelete.getSelection())
                    action = CompileErrorAction.delete;
                else if (btnSkip.getSelection())
                    action = CompileErrorAction.skip;
                else if (btnContinue.getSelection())
                    action = CompileErrorAction.build;
            }
        };
        btnDelete.addSelectionListener(errorActionListener);
        btnSkip.addSelectionListener(errorActionListener);
        btnContinue.addSelectionListener(errorActionListener);

        SelectionAdapter classpathPrefListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (btnExposeEclipseClasspath.getSelection())
                    classpathPref = EclipseClasspathPreference.expose;
                else if (btnHideEclipseClasspath.getSelection())
                    classpathPref = EclipseClasspathPreference.hide;
            }
        };
        btnExposeEclipseClasspath.addSelectionListener(classpathPrefListener);
        btnHideEclipseClasspath.addSelectionListener(classpathPrefListener);

        return container;
    }

    IProject getProject() {
        IAdaptable elem = getElement();
        if (elem instanceof IProject)
            return (IProject) elem;

        IProject project = (IProject) elem.getAdapter(IProject.class);
        if (project != null)
            return project;

        throw new IllegalArgumentException("Target element does not adapt to IProject");
    }

    @Override
    public boolean performOk() {
        IPreferenceStore store = getPreferenceStore();
        store.setValue(CompileErrorAction.PREFERENCE_KEY, action.name());
        store.setValue(EclipseClasspathPreference.PREFERENCE_KEY, classpathPref.name());

        return true;
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return new ScopedPreferenceStore(new ProjectScope(getProject()), Plugin.PLUGIN_ID);
    }
}
