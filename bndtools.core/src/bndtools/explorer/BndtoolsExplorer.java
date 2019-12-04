package bndtools.explorer;

import java.beans.PropertyChangeListener;

import org.bndtools.utils.swt.FilterPanelPart;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import bndtools.Plugin;

public class BndtoolsExplorer extends PackageExplorerPart {
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
								return glob.finds(name) >= 0;

							} else if (element instanceof IProject) {
								String name = ((IProject) element).getName();
								return glob.finds(name) >= 0;
							} else
								return true;
						}
					});
				}
			} else {
				glob = null;
			}
			getTreeViewer().refresh();
		};
		filterPart.addPropertyChangeListener(listener);
	}

	@Override
	public void dispose() {
		filterPart.removePropertyChangeListener(listener);
		super.dispose();
	}
}
