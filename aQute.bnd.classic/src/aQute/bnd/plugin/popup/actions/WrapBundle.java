package aQute.bnd.plugin.popup.actions;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

import aQute.bnd.plugin.*;

public class WrapBundle implements IObjectActionDelegate {
	IFile[]		locations;
	public WrapBundle(){}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		try {
			if ( locations!= null) {
				for (int i = 0; i < locations.length; i++) {
					// TODO
					MessageDialog.openInformation(null,"Not Implemented Yet", "TODO implement wrapping");
				}
			}
		}
		catch( Exception e ) {
			Activator.getDefault().error("Could not start Test View", e );
		}
	}

	 
	 /**
		 * @see IActionDelegate#selectionChanged(IAction, ISelection)
		 */
	public void selectionChanged(IAction action, ISelection selection) {
		locations = getLocations(selection);
	}


	@SuppressWarnings("unchecked")
    IFile[] getLocations(ISelection selection) {
		if (selection != null && (selection instanceof StructuredSelection)) {
			StructuredSelection ss = (StructuredSelection) selection;
			IFile[] result = new IFile[ss.size()];
			int n = 0;
			for (Iterator<IFile> i = ss.iterator(); i.hasNext();) {
				result[n++] = i.next();
			}
			return result;
		}
		return null;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

}
