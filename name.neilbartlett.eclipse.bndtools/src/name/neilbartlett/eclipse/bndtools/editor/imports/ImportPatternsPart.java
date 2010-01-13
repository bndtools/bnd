package name.neilbartlett.eclipse.bndtools.editor.imports;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;

public class ImportPatternsPart extends SectionPart implements PropertyChangeListener {

	private IManagedForm managedForm;
	private TableViewer viewer;
	private BndEditModel model;

	public ImportPatternsPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}
	
	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Import Package Patterns");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		Table table = toolkit.createTable(composite, SWT.MULTI | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn col;
		col = new TableColumn(table, SWT.NONE);
		col.setText("Pattern");
		col.setWidth(130);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Optional");
		col.setWidth(60);
		
		col = new TableColumn(table, SWT.NONE);
		col.setText("Attributes");
		col.setWidth(130);
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ImportPatternTableLabelProvider());
		
		final Button btnAdd = toolkit.createButton(composite, "Add", SWT.PUSH);
		final Button btnRemove = toolkit.createButton(composite, "Remove", SWT.PUSH);
		
		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				managedForm.fireSelectionChanged(ImportPatternsPart.this, event.getSelection());
				btnRemove.setEnabled(!event.getSelection().isEmpty());
			}
		});
		
		// Layout
		GridLayout layout;
		layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		btnAdd.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		btnRemove.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		
		this.managedForm = form;
		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(Constants.IMPORT_PACKAGE, this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		this.model.removePropertyChangeListener(Constants.IMPORT_PACKAGE, this);
	}
	
	@Override
	public void refresh() {
		super.refresh();
		Collection<ImportPattern> patterns = model.getImportPatterns();
		viewer.setInput(patterns);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) managedForm.getContainer();
		if(page.isActive())
			refresh();
		else
			markStale();
	}
}
