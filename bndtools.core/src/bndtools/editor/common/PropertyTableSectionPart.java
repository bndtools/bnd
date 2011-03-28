package bndtools.editor.common;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.model.BndEditModel;

public abstract class PropertyTableSectionPart extends SectionPart implements PropertyChangeListener {

	private final String propertyName;
	private Map<String,String> properties;
	private BndEditModel model;

	private Table table;
	private TableViewer viewer;
	private MapEntryCellModifier<String, String> modifierProperties;

	public PropertyTableSectionPart(String propertyName, Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		this.propertyName = propertyName;

		createSection(getSection(), toolkit);
	}

	void createSection(Section section, FormToolkit toolkit) {
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

		table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewer = new TableViewer(table);
		modifierProperties = new MapEntryCellModifier<String, String>(viewer);

		table.setHeaderVisible(true);
		table.setLinesVisible(false);

		modifierProperties.addColumnsToTable();

		viewer.setUseHashlookup(true);
		viewer.setColumnProperties(modifierProperties.getColumnProperties());
		modifierProperties.addCellEditorsToViewer();
		viewer.setCellModifier(modifierProperties);

		viewer.setContentProvider(new MapContentProvider());
		viewer.setLabelProvider(new PropertiesTableLabelProvider());

		// Layout
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 100;
		table.setLayoutData(gd);

		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeItem.setEnabled(!viewer.getSelection().isEmpty());
			}
		});
		addItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddProperty();
			}
		});
		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemoveProperty();
			}
		});
		modifierProperties.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				markDirty();
			}
		});
	}
	void doAddProperty() {
        properties.put("name", "");
        viewer.add("name");
		markDirty();

        viewer.editElement("name", 0);
	}
	void doRemoveProperty() {
		@SuppressWarnings("rawtypes")
		Iterator iter = ((IStructuredSelection) viewer.getSelection()).iterator();
		while(iter.hasNext()) {
			Object item = iter.next();
			properties.remove(item);
			viewer.remove(item);
		}
		markDirty();
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
		if(model != null)
			model.removePropertyChangeListener(propertyName, this);
	}
	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		if(page.isActive()) {
			refresh();
		} else {
			markStale();
		}
	}
	@Override
	public void refresh() {
		Map<String, String> tmp = loadProperties(model);
		if(tmp == null) {
			this.properties = new HashMap<String, String>();
		} else {
			this.properties = new HashMap<String, String>(tmp);
		}
		viewer.setInput(properties);
		super.refresh();
	}
	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		saveProperties(model, properties);
	}

	protected abstract Map<String, String> loadProperties(BndEditModel model);

	protected abstract void saveProperties(BndEditModel model, Map<String, String> props);
}
