package name.neilbartlett.eclipse.bndtools.editor;

import name.neilbartlett.eclipse.bndtools.editor.components.ComponentsPage;
import name.neilbartlett.eclipse.bndtools.editor.exports.ExportPatternsPage;
import name.neilbartlett.eclipse.bndtools.editor.imports.ImportPatternsPage;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ExportedPackage;
import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponent;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class BndEditorContentOutlinePage extends ContentOutlinePage {
	
	private final BndEditModel model;
	private final BndEditor editor;

	public BndEditorContentOutlinePage(BndEditor editor, BndEditModel model) {
		this.editor = editor;
		this.model = model;
	}
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		TreeViewer viewer = getTreeViewer();
		viewer.setAutoExpandLevel(2);
		viewer.setContentProvider(new BndEditorContentOutlineProvider(viewer));
		viewer.setLabelProvider(new BndEditorContentOutlineLabelProvider());
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object element = selection.getFirstElement();
				
				if(element instanceof String) {
					editor.setActivePage((String) element);
				} else if(element instanceof ServiceComponent) {
					ComponentsPage componentsPage = (ComponentsPage) editor.setActivePage(BndEditor.COMPONENTS_PAGE);
					if(componentsPage != null) {
						componentsPage.setSelectedComponent((ServiceComponent) element);
					}
				} else if(element instanceof ExportedPackage) {
					ExportPatternsPage exportsPage = (ExportPatternsPage) editor.setActivePage(BndEditor.EXPORTS_PAGE);
					if(exportsPage != null) {
						exportsPage.setSelectedExport((ExportedPackage) element);
					}
				} else if(element instanceof ImportPattern) {
					ImportPatternsPage importsPage = (ImportPatternsPage) editor.setActivePage(BndEditor.IMPORTS_PAGE);
					if(importsPage != null) {
						importsPage.setSelectedImport((ImportPattern) element);
					}
				}
			}
		});
		
		viewer.setInput(model);
	}
}
