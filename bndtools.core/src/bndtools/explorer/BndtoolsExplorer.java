package bndtools.explorer;

import java.beans.PropertyChangeListener;
import java.util.Objects;

import org.bndtools.utils.swt.FilterPanelPart;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import bndtools.Plugin;

public class BndtoolsExplorer extends PackageExplorerPart {
	public static final String		VIEW_ID		= "bndtools.PackageExplorer";

	private final FilterPanelPart	filterPart	= new FilterPanelPart(Plugin.getDefault()
		.getScheduler());
	private PropertyChangeListener	listener;
	private Glob					glob;
	boolean							installed;

	@Override
	public void createPartControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout sl = new GridLayout(1, true);
		c.setLayout(sl);
		filterPart.createControl(c);

		super.createPartControl(c);
		Control[] children = c.getChildren();
		if (children.length > 1) {
			children[1].setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
		c.layout();


		filterPart.setHint("Filter for projects (glob)");
		listener = e -> {
			String value = (String) e.getNewValue();

			if (Strings.nonNullOrEmpty(value)) {
				glob = new Glob(value);
				if (!installed) {
					installed = true;
					getTreeViewer().addFilter(new ViewerFilter() {

						@Override
						public boolean select(Viewer viewer, Object parentElement, Object element) {
							if (glob == null)
								return true;

							if (element instanceof JavaProject) {
								IJavaProject project = (JavaProject) element;
								String name = project.getElementName();
								return isSelected(project.getProject(), name);

							} else if (element instanceof IProject) {
								IProject project = (IProject) element;
								String name = project.getName();
								return isSelected(project, name);
							} else
								return true;
						}

						private boolean isSelected(IProject project, String name) {
							if (glob.finds(name) >= 0)
								return true;

							try {
								int maxSeverity = project.findMaxProblemSeverity(null, false, IResource.DEPTH_INFINITE);

								switch (maxSeverity) {
									case 0 :
										return false;
									case IMarker.SEVERITY_ERROR :
										return glob.finds(":error") >= 0;

									case IMarker.SEVERITY_WARNING :
										return glob.finds(":warning") >= 0;
								}
							} catch (CoreException e) {
								// ignore
							}

							return false;

						}
					});
				}
			} else {
				glob = null;
			}
			getTreeViewer().refresh();
		};
		filterPart.addPropertyChangeListener(listener);

		IActionBars actionBars = getViewSite().getActionBars();

		IAction originalPaste = actionBars.getGlobalActionHandler(ActionFactory.PASTE.getId());

		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), new Action() {
			@Override
			public void runWithEvent(Event event) {
				Text filterText = filterPart.getFilterControl();

				if (Objects.equals(event.widget, filterText)) {
					filterText.paste();
				} else {
					originalPaste.runWithEvent(event);
				}
			}
		});
	}

	@Override
	public void dispose() {
		filterPart.removePropertyChangeListener(listener);
		super.dispose();
	}
}
