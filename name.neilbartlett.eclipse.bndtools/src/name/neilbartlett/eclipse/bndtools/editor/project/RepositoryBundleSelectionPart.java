package name.neilbartlett.eclipse.bndtools.editor.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.VersionedClause;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public abstract class RepositoryBundleSelectionPart extends SectionPart implements PropertyChangeListener {

	private final String propertyName;
	private Table table;
	private TableViewer viewer;
	
	private BndEditModel model;
	private List<VersionedClause> bundles;

	protected RepositoryBundleSelectionPart(String propertyName, Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		this.propertyName = propertyName;
		createSection(getSection(), toolkit);
	}
	protected void createSection(Section section, FormToolkit toolkit) {
		// Toolbar buttons
		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);
		final ToolItem addItem = new ToolItem(toolbar, SWT.PUSH);
		addItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
		addItem.setToolTipText("Add Bundle");

		final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
		removeItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		removeItem.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeItem.setToolTipText("Remove");
		removeItem.setEnabled(false);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);
		table.setLinesVisible(false);
		
		TableColumn col;
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Bundle");
		col.setWidth(200);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Version");
		col.setWidth(150);
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new VersionedClauseLabelProvider());
		
		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeItem.setEnabled(!viewer.getSelection().isEmpty());
			}
		});
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
		
		// Layout
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0; layout.verticalSpacing = 0;
		layout.marginHeight = 0; layout.marginWidth = 0;
		composite.setLayout(layout);
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 100;
		table.setLayoutData(gd);
	}
	private void doAdd() {
		List<VersionedClause> copy = new ArrayList<VersionedClause>(bundles);
		RepoBundleSelectionWizard wizard = new RepoBundleSelectionWizard(copy);
		WizardDialog dialog = new WizardDialog(getSection().getShell(), wizard);
		if(dialog.open() == Window.OK) {
			bundles = copy;
			viewer.setInput(bundles);
			markDirty();
		}
	}
	private void doRemove() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if(!selection.isEmpty()) {
			Iterator<?> elements = selection.iterator();
			List<Object> removed = new LinkedList<Object>();
			while(elements.hasNext()) {
				Object element = elements.next();
				if(bundles.remove(element))
					removed.add(element);
			}
			
			if(!removed.isEmpty()) {
				viewer.remove(removed.toArray(new Object[removed.size()]));
				markDirty();
			}
		}
	}
	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		saveToModel(model, bundles);
	}
	
	protected abstract void saveToModel(BndEditModel model, List<VersionedClause> bundles);
	protected abstract List<VersionedClause> loadFromModel(BndEditModel model);
	
	@Override
	public void refresh() {
		List<VersionedClause> bundles = loadFromModel(model);
		if(bundles != null) {
			this.bundles = new ArrayList<VersionedClause>(bundles);
		} else {
			this.bundles = new ArrayList<VersionedClause>();
		}
		viewer.setInput(this.bundles);
		super.refresh();
	}
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		
		model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(propertyName, this);
	}
	@Override
	public void dispose() {
		super.dispose();
		if(model != null) model.removePropertyChangeListener(propertyName, this);
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