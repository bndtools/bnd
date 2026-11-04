package bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;

import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;
import bndtools.editor.BndEditor;
import bndtools.editor.common.BndEditorPart;
import bndtools.model.repo.LoadingContentElement;
import bndtools.utils.ModificationLock;

public class RunFrameworkPart extends BndEditorPart implements PropertyChangeListener {
	private static final String[]				PROPERTIES			= new String[] {
		Constants.RUNFW, Constants.RUNEE
	};

	private final ModificationLock				lock				= new ModificationLock();
	private final OSGiFrameworkContentProvider	fwkContentProvider	= new OSGiFrameworkContentProvider();

	private String								selectedFramework	= null;
	private EE									selectedEE			= null;
	private boolean								inheritedFramework;
	private boolean								inheritedEE;

	private Combo								cmbFramework;
	private Combo								cmbExecEnv;
	private Text								inheritedFrameworkText;
	private Text								inheritedEEText;
	private ComboViewer							eeViewer;
	private StructuredViewer					frameworkViewer;

	private boolean								committing			= false;

	public RunFrameworkPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		fwkContentProvider.onContentReady(list -> Display.getDefault()
			.asyncExec(this::refreshFromModel));

		frameworkViewer.setInput(model.getWorkspace());
	}

	final void createSection(Section section, FormToolkit tk) {
		section.setText("Core Runtime");

		Composite composite = tk.createComposite(section);
		section.setClient(composite);

		Label lblFramework = new Label(composite, SWT.NONE);
		tk.adapt(lblFramework, true, true);
		lblFramework.setText("OSGi Framework:");

		Composite frameworkField = tk.createComposite(composite);
		frameworkField.setLayout(new StackLayout());
		cmbFramework = new Combo(frameworkField, SWT.DROP_DOWN);
		tk.adapt(cmbFramework);
		tk.paintBordersFor(cmbFramework);
		inheritedFrameworkText = createInheritedText(frameworkField, tk, Constants.RUNFW);
		((StackLayout) frameworkField.getLayout()).topControl = cmbFramework;

		frameworkViewer = new ComboViewer(cmbFramework);
		frameworkViewer.setUseHashlookup(true);
		frameworkViewer.setContentProvider(fwkContentProvider);

		Label lblExecEnv = tk.createLabel(composite, "Execution Env.:");
		Composite environmentField = tk.createComposite(composite);
		environmentField.setLayout(new StackLayout());
		cmbExecEnv = new Combo(environmentField, SWT.DROP_DOWN | SWT.READ_ONLY);
		inheritedEEText = createInheritedText(environmentField, tk, Constants.RUNEE);
		((StackLayout) environmentField.getLayout()).topControl = cmbExecEnv;
		ControlDecoration eeDecor = new ControlDecoration(environmentField, SWT.LEFT | SWT.TOP, composite);
		eeDecor.setImage(FieldDecorationRegistry.getDefault()
			.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
			.getImage());
		eeDecor.setDescriptionText(
			"The runtime Java Virtual Machine will be required/assumed " + "\nto support this Execution Environment");

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
		cmbFramework.addModifyListener(e -> lock.ifNotModifying(() -> {
			String text = cmbFramework.getText();
			if (text != null && text.length() > 0 && !LoadingContentElement.MSG.equals(text)) {
				markDirty();
				selectedFramework = text;
			}
		}));
		frameworkViewer.addSelectionChangedListener(event -> lock.ifNotModifying(() -> {
			Object element = ((IStructuredSelection) frameworkViewer.getSelection()).getFirstElement();
			if (element instanceof LoadingContentElement) {
				return;
			}

			markDirty();
			if (element == null)
				selectedFramework = null;
			else
				selectedFramework = element.toString();
		}));
		eeViewer.addSelectionChangedListener(event -> lock.ifNotModifying(() -> {
			markDirty();
			selectedEE = (EE) ((IStructuredSelection) event.getSelection()).getFirstElement();
		}));

		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 10;
		composite.setLayout(layout);

		lblFramework.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		GridData gd;
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 20;
		gd.heightHint = 20;
		frameworkField.setLayoutData(gd);

		lblExecEnv.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 20;
		gd.heightHint = 20;
		environmentField.setLayoutData(gd);
	}

	private Text createInheritedText(Composite parent, FormToolkit toolkit, String key) {
		Text text = toolkit.createText(parent, "", SWT.READ_ONLY);
		text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		text.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent event) {
				BndEditModelAccessor.getScalarPropertyProvenance(model, key)
					.ifPresent(RunFrameworkPart.this::openProvenanceByPath);
			}
		});
		return text;
	}

	@Override
	protected String[] getProperties() {
		return PROPERTIES;
	}

	@Override
	protected void refreshFromModel() {
		lock.modifyOperation(() -> {
			selectedFramework = model.getRunFw();
			String framework = BndEditModelAccessor.getInheritedScalarValue(model, Constants.RUNFW);
			inheritedFramework = selectedFramework == null && framework != null;
			cmbFramework.setText(inheritedFramework ? framework : selectedFramework != null ? selectedFramework : "");
			refreshInheritedState(cmbFramework, inheritedFrameworkText, Constants.RUNFW, inheritedFramework, framework);

			selectedEE = model.getEE();
			String environment = BndEditModelAccessor.getInheritedScalarValue(model, Constants.RUNEE);
			inheritedEE = selectedEE == null && environment != null;
			EE displayedEE = inheritedEE ? EE.parse(environment) : selectedEE;
			eeViewer.setSelection(displayedEE != null ? new StructuredSelection(displayedEE) : StructuredSelection.EMPTY);
			refreshInheritedState(cmbExecEnv, inheritedEEText, Constants.RUNEE, inheritedEE,
				displayedEE != null ? displayedEE.getEEName() : environment);
		});
	}

	private void refreshInheritedState(Combo combo, Text text, String key, boolean inherited, String value) {
		text.setText(inherited && value != null ? value : "");
		text.setToolTipText(inherited ? BndEditModelAccessor.getScalarPropertyProvenance(model, key)
			.map(path -> "Inherited from " + BndEditModelAccessor.getDisplayProvenance(model, path)
				+ "\nDouble-click to open source.")
			.orElse("Inherited from an included file.") : null);
		Composite parent = combo.getParent();
		((StackLayout) parent.getLayout()).topControl = inherited ? text : combo;
		parent.layout();
	}

	private void openProvenanceByPath(String absolutePath) {
		File file = new File(absolutePath);
		if (!file.isFile())
			return;
		IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(absolutePath));
		if (workspaceFile == null || !workspaceFile.exists())
			return;
		try {
			PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage()
				.openEditor(new FileEditorInput(workspaceFile), BndEditor.WORKSPACE_EDITOR);
		} catch (PartInitException exception) {
			ErrorDialog.openError(getSection().getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to open source file.", exception));
		}
	}

	@Override
	public void commitToModel(boolean onSave) {
		try {
			committing = true;
			if (!inheritedFramework) {
				model.setRunFw(selectedFramework == null ? null
					: selectedFramework.trim()
						.length() > 0 ? selectedFramework.trim() : null);
			}
			if (!inheritedEE) {
				model.setEE(selectedEE);
			}
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

	@Override
	public void dispose() {
		fwkContentProvider.dispose();
		super.dispose();
	}
}
