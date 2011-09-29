package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.BndConstants;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.utils.ModificationLock;

public class RunFrameworkPart extends SectionPart implements PropertyChangeListener {

    private final ModificationLock lock = new ModificationLock();
    private final OSGiFrameworkContentProvider fwkContentProvider = new OSGiFrameworkContentProvider();

    private BndEditModel model;
    private String selectedFramework = null;

    private Combo frameworkCombo;
    private ComboViewer frameworkViewer;

    public RunFrameworkPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createSection(getSection(), toolkit);
    }

    final void createSection(Section section, FormToolkit toolkit) {
        section.setText("Core Runtime");

        Composite composite = toolkit.createComposite(section);
        composite.setLayout(new GridLayout(2, false));

        Label lblFramework = new Label(composite, SWT.NONE);
        lblFramework.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        toolkit.adapt(lblFramework, true, true);
        lblFramework.setText("OSGi Framework:");

        frameworkCombo = new Combo(composite, SWT.DROP_DOWN);
        toolkit.adapt(frameworkCombo);
        toolkit.paintBordersFor(frameworkCombo);

        frameworkCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        frameworkViewer = new ComboViewer(frameworkCombo);
        frameworkViewer.setUseHashlookup(true);
        frameworkViewer.setContentProvider(fwkContentProvider);
        try {
            frameworkViewer.setInput(Central.getWorkspace());
        } catch (Exception e) {
            Plugin.logError("Error accessing bnd workspace.", e);
        }

        frameworkCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        markDirty();
                        selectedFramework = frameworkCombo.getText();
                        validateFramework();
                    }
                });
            }
        });
        frameworkViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                lock.ifNotModifying(new Runnable() {
                    public void run() {
                        markDirty();
                        Object element = ((IStructuredSelection) frameworkViewer.getSelection()).getFirstElement();
                        if (element == null)
                            selectedFramework = null;
                        else
                            selectedFramework = element.toString();
                        validateFramework();
                    }
                });
            }
        });

        /*
        Label lblNewLabel = new Label(composite, SWT.NONE);
        toolkit.adapt(lblNewLabel, true, true);
        lblNewLabel.setText("Execution Environment:");

        Combo eeCombo = new Combo(composite, SWT.READ_ONLY);
        toolkit.adapt(eeCombo);
        toolkit.paintBordersFor(eeCombo);

        eeViewer = new ComboViewer(eeCombo);
        eeViewer.setContentProvider(ArrayContentProvider.getInstance());
        eeViewer.setLabelProvider(new EELabelProvider());
        eeViewer.setInput(EE.values());
        */

        section.setClient(composite);
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);
        model = (BndEditModel) form.getInput();
        model.addPropertyChangeListener(BndConstants.RUNFRAMEWORK, this);
    }

    @Override
    public void dispose() {
        super.dispose();
        if(model != null)
            model.removePropertyChangeListener(BndConstants.RUNFRAMEWORK, this);
    }

    @Override
    public void refresh() {
        lock.modifyOperation(new Runnable() {
            public void run() {
                selectedFramework = model.getRunFramework();
                frameworkCombo.setText(selectedFramework != null ? selectedFramework : "");
                validateFramework();
            }
        });
    }

    void validateFramework() {
        /*
        IMessageManager messages = getManagedForm().getMessageManager();
        if(frameworkStr == null || frameworkStr.length() == 0) {
            messages.addMessage(WARNING_NO_FWK, "No runtime framework specified", null, IMessageProvider.WARNING, frameworkCombo);
            return;
        } else {
            messages.removeMessage(WARNING_NO_FWK, frameworkCombo);
        }
        Map<String, Map<String, String>> header = OSGiHeader.parseHeader(frameworkStr);
        if(header.size() != 1) {
            messages.addMessage("ERROR_INVALID_FWK", "Invalid format", null, IMessageProvider.ERROR, frameworkCombo);
            return;
        } else {
            messages.removeMessage("ERROR_INVALID_FWK", frameworkCombo);
        }
        */
    }

    @Override
    public void commit(boolean onSave) {
        super.commit(onSave);
        model.setRunFramework(selectedFramework);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        IFormPage page = (IFormPage) getManagedForm().getContainer();
        if(page.isActive()) {
            refresh();
        } else {
            markStale();
        }
    }
}
