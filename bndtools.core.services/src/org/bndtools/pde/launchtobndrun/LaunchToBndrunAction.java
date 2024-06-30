package org.bndtools.pde.launchtobndrun;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.osgi.service.component.annotations.Component;

import aQute.lib.io.IO;
import biz.aQute.bnd.pde.launch2bndrun.LaunchToBndrun;

@Component
public class LaunchToBndrunAction implements IObjectActionDelegate {

	IFile[] selected;

	@Override
	public void run(IAction action) {
		if (selected == null) {
			return;
		}
		IFile s = selected[0];

		try {
			LaunchToBndrun lbr = new LaunchToBndrun(5, s.getContents());
			String name = s.getName()
				.replaceAll("\\.launch$", ".bndrun");

			IFile f = s.getParent()
				.getFile(new Path(name));
			f.create(IO.stream(lbr.getDoc()
				.get()), true, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection == null || !(selection instanceof StructuredSelection)) {
			selected = null;
			return;
		}
		StructuredSelection ss = (StructuredSelection) selection;
		selected = new IFile[ss.size()];
		int n = 0;
		for (@SuppressWarnings("unchecked")
		Iterator<IFile> i = ss.iterator(); i.hasNext();) {
			selected[n++] = i.next();
		}
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub

	}

}
