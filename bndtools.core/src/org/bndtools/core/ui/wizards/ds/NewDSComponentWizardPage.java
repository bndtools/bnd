package org.bndtools.core.ui.wizards.ds;

import java.util.EnumSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

public class NewDSComponentWizardPage extends NewTypeWizardPage {

    static enum ActivateSignature {
        NoActivate("No activate/deactivate methods"), NoArg("Activate without arguments"), ConfigMap("Activate with configuration map"), ComponentContext("Activate with ComponentContext"), BundleContext("Activate with BundleContext");

        String label;

        ActivateSignature(String label) {
            this.label = label;
        }
    }

    private static final String PAGE_NAME = "NewDSComponentWizardPage";

    private final static String SETTINGS_CREATECONSTR = "create_constructor"; //$NON-NLS-1$
    private final static String SETTINGS_CREATEUNIMPLEMENTED = "create_unimplemented"; //$NON-NLS-1$

    private final SelectionButtonDialogFieldGroup fMethodStubsButtons;
    private ComboViewer vwrActivateStub;

    private ActivateSignature activateSignature = ActivateSignature.NoActivate;
    private IStatus activateSignatureStatus = Status.OK_STATUS;

    private IType fCreatedType;

    public NewDSComponentWizardPage() {
        super(CLASS_TYPE, PAGE_NAME);
        setTitle("Declarative Services Component Class");
        setDescription("Create a new Declarative Services component class.");

        String[] buttonNames3 = new String[] {
                NewWizardMessages.NewClassWizardPage_methods_constructors, NewWizardMessages.NewClassWizardPage_methods_inherited
        };
        fMethodStubsButtons = new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames3, 1);
        fMethodStubsButtons.setLabelText(NewWizardMessages.NewClassWizardPage_methods_label);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            setFocus();
        }
    }

    @Override
    protected String getSuperInterfacesLabel() {
        return "Service Interfaces:";
    }

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());

        int nColumns = 4;

        GridLayout layout = new GridLayout();
        layout.numColumns = nColumns;
        composite.setLayout(layout);

        // pick & choose the wanted UI components

        createContainerControls(composite, nColumns);
        createPackageControls(composite, nColumns);
        createEnclosingTypeControls(composite, nColumns);

        createSeparator(composite, nColumns);

        createTypeNameControls(composite, nColumns);
        createLifecycleMethodStubControls(composite, nColumns);
        createSuperInterfacesControls(composite, nColumns);
        createSuperClassControls(composite, nColumns);
        createMethodStubSelectionControls(composite, nColumns);

        setControl(composite);

        Dialog.applyDialogFont(composite);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE);
    }

    public void init(IStructuredSelection selection) {
        IJavaElement jelem = getInitialJavaElement(selection);
        initContainerPage(jelem);
        initTypePage(jelem);
        doStatusUpdate();

        boolean createConstructors = false;
        boolean createUnimplemented = true;
        IDialogSettings dialogSettings = getDialogSettings();
        if (dialogSettings != null) {
            IDialogSettings section = dialogSettings.getSection(PAGE_NAME);
            if (section != null) {
                createConstructors = section.getBoolean(SETTINGS_CREATECONSTR);
                createUnimplemented = section.getBoolean(SETTINGS_CREATEUNIMPLEMENTED);
            }
        }

        setMethodStubSelection(createConstructors, createUnimplemented, true);
    }

    @Override
    protected void handleFieldChanged(String fieldName) {
        super.handleFieldChanged(fieldName);

        doStatusUpdate();
    }

    private void doStatusUpdate() {
        // status of all used components
        IStatus[] status = new IStatus[] {
                fContainerStatus, isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus, fTypeNameStatus, fSuperClassStatus, fSuperInterfacesStatus, activateSignatureStatus
        };

        // the mode severe status will be displayed and the OK button enabled/disabled.
        updateStatus(status);
    }

    public void setMethodStubSelection(boolean createConstructors, boolean createInherited, boolean canBeModified) {
        fMethodStubsButtons.setSelection(0, createConstructors);
        fMethodStubsButtons.setSelection(1, createInherited);

        fMethodStubsButtons.setEnabled(canBeModified);
    }

    protected void createLifecycleMethodStubControls(Composite composite, int nColumns) {
        // Create stubs for lifecycle methods (activate and deactivate)
        Label lblLifecycleMethods = new Label(composite, SWT.NONE);
        lblLifecycleMethods.setText("Lifecycle Methods:");
        LayoutUtil.setHorizontalSpan(lblLifecycleMethods, 1);

        Combo cmbActivateStub = new Combo(composite, SWT.READ_ONLY);
        vwrActivateStub = new ComboViewer(cmbActivateStub);
        vwrActivateStub.setContentProvider(ArrayContentProvider.getInstance());
        vwrActivateStub.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ActivateSignature) element).label;
            }
        });
        vwrActivateStub.setInput(EnumSet.allOf(ActivateSignature.class));
        vwrActivateStub.setSelection(new StructuredSelection(activateSignature), true);
        vwrActivateStub.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection sel = event.getSelection();
                if (!sel.isEmpty() && sel instanceof IStructuredSelection) {
                    activateSignature = (ActivateSignature) ((IStructuredSelection) sel).getFirstElement();
                } else {
                    activateSignature = ActivateSignature.NoActivate;
                }
                activateSignatureStatus = activateSignatureChanged();
                handleFieldChanged("activateSignature");
            }
        });
        //        LayoutUtil.setHorizontalSpan(cmbActivateStub, nColumns - 1);
        GridData gd = new GridData(SWT.LEFT, SWT.FILL, false, false, nColumns - 1, 1);
        gd.horizontalIndent = -4; // counteract the weird additional space added to combos
        cmbActivateStub.setLayoutData(gd);
    }

    protected IStatus activateSignatureChanged() {
        return Status.OK_STATUS;
    }

    protected void createMethodStubSelectionControls(Composite composite, int nColumns) {
        // Create stubs for constructors and abstract methods
        Control labelControl = fMethodStubsButtons.getLabelControl(composite);
        LayoutUtil.setHorizontalSpan(labelControl, nColumns);

        DialogField.createEmptySpace(composite);

        Control buttonGroup = fMethodStubsButtons.getSelectionButtonsGroup(composite);
        LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1);
    }

    @Override
    public int getModifiers() {
        return F_PUBLIC;
    }

    public boolean isCreateConstructors() {
        return fMethodStubsButtons.isSelected(0);
    }

    public boolean isCreateInherited() {
        return fMethodStubsButtons.isSelected(1);
    }

    @Override
    public IType getCreatedType() {
        return fCreatedType;
    }

    @Override
    protected void constructTypeAnnotationStubs(StringBuffer buf, ImportsManager imports, String lineDelimiter) {
        buf.append("@").append(imports.addImport("aQute.bnd.annotation.component.Component")).append(lineDelimiter);
    }

    @Override
    protected void createTypeMembers(IType newType, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
        boolean doConstr = isCreateConstructors();
        boolean doInherited = isCreateInherited();

        createInheritedMethods(newType, doConstr, doInherited, imports, new SubProgressMonitor(monitor, 1));

        if (activateSignature != null && activateSignature != ActivateSignature.NoActivate) {
            StringBuilder builder;
            String content;
            final String lineDelim = "\n";

            // Generate activate method
            builder = new StringBuilder();
            builder.append("@").append(imports.addImport("aQute.bnd.annotation.component.Activate")).append(lineDelim);
            builder.append("void activate(");

            switch (activateSignature) {
            case NoActivate :
            case NoArg :
            default :
                // Nothing to do
                break;
            case ConfigMap :
                builder.append(imports.addImport("java.util.Map")).append("<").append(imports.addImport("java.lang.String")).append(",").append(imports.addImport("java.lang.Object")).append(">").append(" ").append("configProps");
                break;
            case ComponentContext :
                builder.append(imports.addImport("org.osgi.service.component.ComponentContext")).append(" ").append("componentContext");
                break;
            case BundleContext :
                builder.append(imports.addImport("org.osgi.framework.BundleContext")).append(" ").append("bundleContext");
                break;
            }

            builder.append(") {").append(lineDelim);
            content = CodeGeneration.getMethodBodyContent(newType.getCompilationUnit(), newType.getTypeQualifiedName('.'), "activate", false, "", lineDelim);
            if (content != null && content.length() > 0)
                builder.append(content);
            builder.append("}");
            newType.createMethod(builder.toString(), null, false, null);

            // Generate deactivate method
            builder = new StringBuilder();
            builder.append("@").append(imports.addImport("aQute.bnd.annotation.component.Deactivate")).append(lineDelim);
            builder.append("void deactivate() {").append(lineDelim);
            content = CodeGeneration.getMethodBodyContent(newType.getCompilationUnit(), newType.getTypeQualifiedName('.'), "activate", false, "", lineDelim);
            if (content != null && content.length() > 0)
                builder.append(content);
            builder.append("}");
            newType.createMethod(builder.toString(), null, false, null);

        }

        if (monitor != null)
            monitor.done();
    }
}
