package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

public class RequiredObrIndexWizardPage extends WizardPage {

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private final List<String> urls;
    private List<String> checkedUrls;
    private final String infoMessage;

    private Label label;
    private Table table;
    private CheckboxTableViewer viewer;

    private boolean programmaticChange = false;

    public RequiredObrIndexWizardPage(IProject project, List<String> urls) {
        super("requiredObrPage");
        this.urls = urls;
        this.checkedUrls = new ArrayList<String>(urls);

        infoMessage = String.format("Project \"%s\" requires the following OBR repositories to satisfy its%n" + "dependencies. Checked repository indexes will be added to the Workspace.", project.getName());
    }

    public void setCheckedUrls(List<String> checkedUrls) {
        List<String> old = this.checkedUrls;
        this.checkedUrls = checkedUrls;

        if (!programmaticChange) {
            try {
                programmaticChange = true;
                if (Display.getCurrent() != null && viewer != null && !table.isDisposed()) {
                    viewer.setCheckedElements(checkedUrls.toArray());
                }
            } finally {
                programmaticChange = false;
            }
        }

        propSupport.firePropertyChange("checkedUrls", old, checkedUrls);
    }

    public List<String> getCheckedUrls() {
        return checkedUrls;
    }

    public void createControl(Composite parent) {
        setTitle("Required Repositories");

        // Create controls
        Composite composite = new Composite(parent, SWT.NONE);
        label = new Label(composite, SWT.NONE);
        label.setText("Add to Workspace:");

        table = new Table(composite, SWT.FULL_SELECTION | SWT.CHECK | SWT.BORDER);
        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(urls);
        viewer.setAllChecked(true);

        Button btnCheckAll = new Button(composite, SWT.PUSH);
        btnCheckAll.setText("Check All");

        Button btnUncheckAll = new Button(composite, SWT.PUSH);
        btnUncheckAll.setText("Uncheck All");

        btnCheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                viewer.setAllChecked(true);
                updateUI();
            }
        });
        btnUncheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                viewer.setAllChecked(false);
                updateUI();
            }
        });
        viewer.addCheckStateListener(new ICheckStateListener() {
            @SuppressWarnings({
                    "rawtypes", "unchecked"
            })
            public void checkStateChanged(CheckStateChangedEvent event) {
                if (!programmaticChange) {
                    try {
                        programmaticChange = true;
                        List list = Arrays.asList(viewer.getCheckedElements());
                        setCheckedUrls(list);
                    } finally {
                        programmaticChange = false;
                    }
                }
                updateUI();
            }
        });

        // Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(2, false);
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
        label.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
        table.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        btnCheckAll.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        btnUncheckAll.setLayoutData(gd);

        setControl(composite);
    }

    void updateUI() {
        getContainer().updateMessage();
        getContainer().updateButtons();
        getContainer().updateTitleBar();
    }

    @Override
    public int getMessageType() {
        return checkedUrls.size() < urls.size() ? IMessageProvider.WARNING : IMessageProvider.NONE;
    }

    @Override
    public String getMessage() {
        return checkedUrls.size() < urls.size() ? "The project may fail to build or run if the required OBR indexes are\nnot imported." : infoMessage;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(propertyName, listener);
    }

}
