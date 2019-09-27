package bndtools.editor.common;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SaneDetailsPart implements IFormPart, IPartSelectionListener {

	private StackLayout							stack;
	private Composite							deselectedPanel;

	private IManagedForm						managedForm;
	private FormToolkit							toolkit;
	private Composite							parent;

	private final Map<Class<?>, IDetailsPage>	pageMap			= new HashMap<>(3);
	private final Map<Class<?>, Control>		controlCache	= new HashMap<>(3);
	private IDetailsPage						deselectedPage	= null;

	private IDetailsPage						currentPage		= null;
	private ISelection							currentSelection;
	private IFormPart							masterPart;

	public void registerPage(Class<?> clazz, IDetailsPage page) {
		pageMap.put(clazz, page);
		page.initialize(managedForm);
	}

	public void registerDeselectedPage(IDetailsPage page) {
		this.deselectedPage = page;
	}

	public void createContents(FormToolkit toolkit, Composite parent) {
		this.toolkit = toolkit;
		this.parent = parent;
		stack = new StackLayout();
		parent.setLayout(stack);

		deselectedPanel = toolkit.createComposite(parent);
		// deselectedPanel.setBackground(new Color(parent.getDisplay(), 255, 0,
		// 0));
		if (deselectedPage != null)
			deselectedPage.createContents(deselectedPanel);
		stack.topControl = deselectedPanel;
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		masterPart = part;
		currentSelection = selection;

		Class<?> clazz = null;
		if (selection instanceof IStructuredSelection) {
			Object selected = ((IStructuredSelection) selection).getFirstElement();
			if (selected != null)
				clazz = selected.getClass();
		}
		showPage(clazz);
	}

	void showPage(Class<?> clazz) {
		IDetailsPage oldPage = currentPage;

		currentPage = clazz != null ? currentPage = pageMap.get(clazz) : null;

		// Save data from old page
		if (oldPage != null && oldPage.isDirty())
			oldPage.commit(false);

		// Show control
		Control control;
		if (currentPage == null) {
			control = deselectedPanel;
		} else {
			control = controlCache.get(clazz);
			if (control == null) {
				Composite composite = toolkit.createComposite(parent);
				controlCache.put(clazz, composite);
				currentPage.createContents(composite);
				control = composite;
			}
			currentPage.selectionChanged(masterPart, currentSelection);
		}
		stack.topControl = control;
		parent.layout();

		// Refresh
		if (currentPage != null && currentPage.isStale())
			currentPage.refresh();
	}

	@Override
	public void commit(boolean onSave) {
		if (currentPage != null)
			currentPage.commit(onSave);
	}

	@Override
	public void dispose() {
		for (Class<?> key : pageMap.keySet()) {
			controlCache.remove(key);
			IDetailsPage page = pageMap.get(key);
			page.dispose();
		}
	}

	@Override
	public void initialize(IManagedForm form) {
		this.managedForm = form;
	}

	@Override
	public boolean isDirty() {
		boolean dirty = currentPage != null && currentPage.isDirty();
		return dirty;
	}

	@Override
	public boolean isStale() {
		return currentPage != null && currentPage.isStale();
	}

	@Override
	public void refresh() {
		if (currentPage != null)
			currentPage.refresh();
	}

	@Override
	public void setFocus() {
		if (currentPage != null)
			currentPage.setFocus();
	}

	@Override
	public boolean setFormInput(Object input) {
		return false;
	}
}
