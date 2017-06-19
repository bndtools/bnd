package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import aQute.bnd.build.model.EE;
import bndtools.BndConstants;
import bndtools.utils.ModificationLock;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.LoadingContentElement;

public class RunFrameworkPart extends BndEditorPart implements PropertyChangeListener {
    private static final String[] PROPERTIES = new String[] {
            BndConstants.RUNFW, BndConstants.RUNEE
    };

    private final ModificationLock lock = new ModificationLock();
    private final OSGiFrameworkContentProvider fwkContentProvider = new OSGiFrameworkContentProvider();

    private String selectedFramework = null;
    private EE selectedEE = null;

    private Combo cmbFramework;
    private Combo cmbExecEnv;
    private ComboViewer eeViewer;
    private StructuredViewer frameworkViewer;

    private boolean committing = false;

    public RunFrameworkPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createSection(getSection(), toolkit);
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);

        fwkContentProvider.onContentReady(new Success<List<OSGiFramework>,Void>() {
            @Override
            public Promise<Void> call(Promise<List<OSGiFramework>> resolved) throws Exception {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        refreshFromModel();
                    }
                });
                return null;
            }
        });

        frameworkViewer.setInput(model.getWorkspace());
    }

    final void createSection(Section section, FormToolkit tk) {
        section.setText("Core Runtime");

        Composite composite = tk.createComposite(section);
        section.setClient(composite);

        Label lblFramework = new Label(composite, SWT.NONE);
        tk.adapt(lblFramework, true, true);
        lblFramework.setText("OSGi Framework:");

        cmbFramework = new Combo(composite, SWT.DROP_DOWN);
        tk.adapt(cmbFramework);
        tk.paintBordersFor(cmbFramework);

        cmbFramework.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        frameworkViewer = new ComboViewer(cmbFramework);
        frameworkViewer.setUseHashlookup(true);
        frameworkViewer.setContentProvider(fwkContentProvider);

        Label lblExecEnv = tk.createLabel(composite, "Execution Env.:");
        cmbExecEnv = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        ControlDecoration eeDecor = new ControlDecoration(cmbExecEnv, SWT.LEFT | SWT.TOP, composite);
        eeDecor.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
        eeDecor.setDescriptionText("The runtime Java Virtual Machine will be required/assumed " + "\nto support this Execution Environment");

        eeViewer = new ComboViewer(cmbExecEnv);
        eeViewer.setContentProvider(ArrayContentProvider.getInstance());
        eeViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((EE) element).getEEName();
            }
        });
        eeViewer.setInput(EE.values());

        // Listeners
        cmbFramework.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                lock.ifNotModifying(new Runnable() {
                    @Override
                    public void run() {
                        String text = cmbFramework.getText();
                        if (text != null && text.length() > 0 && !LoadingContentElement.MSG.equals(text)) {
                            markDirty();
                            selectedFramework = text;
                        }
                    }
                });
            }
        });
        frameworkViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                lock.ifNotModifying(new Runnable() {
                    @Override
                    public void run() {
                        Object element = ((IStructuredSelection) frameworkViewer.getSelection()).getFirstElement();
                        if (element instanceof LoadingContentElement) {
                            return;
                        }

                        markDirty();
                        if (element == null)
                            selectedFramework = null;
                        else
                            selectedFramework = element.toString();
                    }
                });
            }
        });
        eeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                lock.ifNotModifying(new Runnable() {
                    @Override
                    public void run() {
                        markDirty();
                        selectedEE = (EE) ((IStructuredSelection) event.getSelection()).getFirstElement();
                    }
                });
            }
        });

        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        composite.setLayout(layout);

        lblFramework.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        GridData gd;
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.widthHint = 20;
        gd.heightHint = 20;
        cmbFramework.setLayoutData(gd);

        lblExecEnv.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.widthHint = 20;
        gd.heightHint = 20;
        cmbExecEnv.setLayoutData(gd);
    }

    @Override
    protected String[] getProperties() {
        return PROPERTIES;
    }

    @Override
    protected void refreshFromModel() {
        lock.modifyOperation(new Runnable() {
            @Override
            public void run() {
                selectedFramework = model.getRunFw();
                if (selectedFramework != null)
                    cmbFramework.setText(selectedFramework);

                selectedEE = model.getEE();
                eeViewer.setSelection(selectedEE != null ? new StructuredSelection(selectedEE) : StructuredSelection.EMPTY);
            }
        });
    }

    @Override
    public void commitToModel(boolean onSave) {
        try {
            committing = true;
            model.setRunFw(selectedFramework.trim().length() > 0 ? selectedFramework.trim() : null);
            model.setEE(selectedEE);
        } finally {
            committing = false;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!committing) {
            IFormPage page = (IFormPage) getManagedForm().getContainer();
            if (page.isActive()) {
                refresh();
            } else {
                markStale();
            }
        }
    }
}
