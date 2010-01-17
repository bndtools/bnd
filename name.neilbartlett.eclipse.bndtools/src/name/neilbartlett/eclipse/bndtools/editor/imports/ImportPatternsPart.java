package name.neilbartlett.eclipse.bndtools.editor.imports;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;
import name.neilbartlett.eclipse.bndtools.utils.CollectionUtils;
import name.neilbartlett.eclipse.bndtools.utils.EditorUtils;
import name.neilbartlett.eclipse.bndtools.views.impexp.AnalyseImportsJob;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Constants;

public class ImportPatternsPart extends SectionPart implements PropertyChangeListener {

	private IManagedForm managedForm;
	private TableViewer viewer;
	private BndEditModel model;
	private List<ImportPattern> patterns;
	
	private final Image imgAnalyse = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/cog_go.png").createImage();
	
	private final IAction fixMissingStarPatternAction = new Action("Append missing \"*\" pattern.") {
		public void run() {
			ImportPattern starPattern = new ImportPattern("*", new HashMap<String, String>());
			patterns.add(starPattern);
			viewer.add(starPattern);
			validate();
			markDirty();
		}
	};

	public ImportPatternsPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}
	
	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Import Package Patterns");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		Table table = toolkit.createTable(composite, SWT.MULTI | SWT.FULL_SELECTION);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ImportPatternLabelProvider());
		
		final Button btnAdd = toolkit.createButton(composite, "Add...", SWT.PUSH);
		final Button btnInsert = toolkit.createButton(composite, "Insert", SWT.PUSH);
		final Button btnRemove = toolkit.createButton(composite, "Remove", SWT.PUSH);
		final Button btnMoveUp = toolkit.createButton(composite, "Up", SWT.PUSH);
		final Button btnMoveDown = toolkit.createButton(composite, "Down", SWT.PUSH);
		toolkit.createLabel(composite, ""); // Spacer
		
		ImageHyperlink lnkAnalyse = toolkit.createImageHyperlink(composite, SWT.LEFT);
		lnkAnalyse.setText("Analyse Imports");
		lnkAnalyse.setImage(imgAnalyse);

		
		// Listeners
		lnkAnalyse.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				IFormPage formPage = (IFormPage) getManagedForm().getContainer();
				IFile file = ResourceUtil.getFile(formPage.getEditorInput());
				
				FormEditor editor = formPage.getEditor();
				if(EditorUtils.saveEditorIfDirty(editor, "Analyse Imports", "The editor content must be saved before continuing.")) {
					AnalyseImportsJob job = new AnalyseImportsJob("Analyse Imports", file, formPage.getEditorSite().getPage());
					job.schedule();
				}
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				managedForm.fireSelectionChanged(ImportPatternsPart.this, event.getSelection());
				boolean enabled = !viewer.getSelection().isEmpty();
				btnInsert.setEnabled(enabled);
				btnRemove.setEnabled(enabled);
				btnMoveUp.setEnabled(enabled);
				btnMoveDown.setEnabled(enabled);
			}
		});
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		btnInsert.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doInsert();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
		btnMoveUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doMoveUp();
			}
		});
		btnMoveDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doMoveDown();
			}
		});

		
		// Layout
		GridLayout layout;
		layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		lnkAnalyse.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 6));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnInsert.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	void doAdd() {
		boolean appendStar = patterns.isEmpty();
		
		ImportPattern newPattern = new ImportPattern("", new HashMap<String, String>());
		patterns.add(newPattern);
		viewer.add(newPattern);
		
		if(appendStar) {
			ImportPattern starPattern = new ImportPattern("*", new HashMap<String, String>());
			patterns.add(starPattern);
			viewer.add(starPattern);
		}
		
		viewer.setSelection(new StructuredSelection(newPattern));
		validate();
		markDirty();
	}
	void doInsert() {
		int selectedIndex;
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if(selection.isEmpty())
			return;
		
		selectedIndex = patterns.indexOf(selection.getFirstElement());
		ImportPattern pattern = new ImportPattern("", new HashMap<String, String>());

		patterns.add(selectedIndex, pattern);
		viewer.insert(pattern, selectedIndex);
		
		viewer.setSelection(new StructuredSelection(pattern));
		validate();
		markDirty();
	}
	void doRemove() {
		@SuppressWarnings("unchecked")
		Iterator iter = ((IStructuredSelection) viewer.getSelection()).iterator();
		while(iter.hasNext()) {
			Object item = iter.next();
			patterns.remove(item);
			viewer.remove(item);
		}
		validate();
		markDirty();
	}
	void doMoveUp() {
		int[] selectedIndexes = findSelectedIndexes();
		int[] newSelection = CollectionUtils.moveUp(patterns, selectedIndexes);
		viewer.refresh();
		//resetSelection(newSelection);
		validate();
		markDirty();
	}
	void doMoveDown() {
		int[] selectedIndexes = findSelectedIndexes();
		int[] newSelection = CollectionUtils.moveDown(patterns, selectedIndexes);
		viewer.refresh();
		//resetSelection(newSelection);
		validate();
		markDirty();
	}
	int[] findSelectedIndexes() {
		Object[] selection = ((IStructuredSelection) viewer.getSelection()).toArray();
		int[] selectionIndexes = new int[selection.length];
		
		for(int i=0; i<selection.length; i++) {
			selectionIndexes[i] = patterns.indexOf(selection[i]);
		}
		return selectionIndexes;
	}
	void resetSelection(int[] selectedIndexes) {
		ArrayList<ImportPattern> selection = new ArrayList<ImportPattern>(selectedIndexes.length);
		for (int index : selectedIndexes) {
			selection.add(patterns.get(index));
		}
		viewer.setSelection(new StructuredSelection(selection), true);
	}
;	@Override
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
		imgAnalyse.dispose();
	}
	@Override
	public void refresh() {
		super.refresh();
		
		// Deep-copy the model
		Collection<ImportPattern> original = model.getImportPatterns();
		if(original != null) {
			patterns = new ArrayList<ImportPattern>(original.size());
			for (ImportPattern pattern : original) {
				patterns.add(pattern.clone());
			}
		} else {
			patterns = new ArrayList<ImportPattern>();
		}
		viewer.setInput(patterns);
		validate();
	}
	
	public void validate() {
		IMessageManager msgs = getManagedForm().getMessageManager();
		msgs.setDecorationPosition(SWT.TOP | SWT.RIGHT);
		
		String noStarWarning = null;
		if(!patterns.isEmpty()) {
			ImportPattern last = patterns.get(patterns.size() - 1);
			if(!last.getName().equals("*"))
				noStarWarning = "The catch-all pattern \"*\" should be present and in the last position.";
		}
		if(noStarWarning != null) {
			msgs.addMessage("_warning_no_star", noStarWarning, fixMissingStarPatternAction, IMessageProvider.WARNING);
		} else {
			msgs.removeMessage("_warning_no_star");
		}
	}
	
	@Override
	public void commit(boolean onSave) {
		try {
			model.removePropertyChangeListener(Constants.IMPORT_PACKAGE, this);
			model.setImportPatterns(patterns.isEmpty() ? null : patterns);
		} finally {
			super.commit(onSave);
			model.addPropertyChangeListener(Constants.IMPORT_PACKAGE, this);
		}
	}
	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) managedForm.getContainer();
		if(page.isActive())
			refresh();
		else
			markStale();
	}
	public void updateLabel(ImportPattern pattern) {
		viewer.update(pattern, null);
	}
}
