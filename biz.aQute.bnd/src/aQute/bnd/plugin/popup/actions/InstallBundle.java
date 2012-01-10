package aQute.bnd.plugin.popup.actions;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;

import aQute.bnd.plugin.*;

public class InstallBundle implements IObjectActionDelegate {
	IFile[]	locations;

	public InstallBundle() {
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		Activator activator = Activator.getDefault();
		Map<String,Bundle> map = new HashMap<String, Bundle>();
		BundleContext context = activator.getBundle().getBundleContext();
		Bundle bundles[] = context.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			map.put(bundles[i].getLocation(), bundles[i]);
		}

		if (locations != null) {
			List<Bundle> installed = new ArrayList<Bundle>();
			List<Bundle> updated = new ArrayList<Bundle>();
			int errors = 0;
			for (int i = 0; i < locations.length; i++) {
				try {
					File mf = locations[i].getLocation().toFile();
					String url = mf.toURI().toURL().toExternalForm();
					Bundle b = (Bundle) map.get(url);
					if (b != null) {
						b.update();
						updated.add(b);
					} else {
						b = context.installBundle(url);
						installed.add(b);
					}
				} catch (Exception e) {
					errors++;
					Activator.getDefault()
							.error("Error during install/update ", e);
				}
			}
			if ( !updated.isEmpty()) {
				ServiceReference ref = context.getServiceReference(PackageAdmin.class.getName());
				if ( ref != null ) {
					PackageAdmin packageAdmin = (PackageAdmin) context.getService(ref);
					if ( packageAdmin != null ) {
						packageAdmin.refreshPackages((Bundle[])updated.toArray(new Bundle[updated.size()]));
					} else
						activator.error("Can't get Package Admin service to refresh", null);
				}  else
					activator.error("No Package Admin to refresh", null);
			}
			StringBuilder sb = new StringBuilder();
			printBundles("Installed Bundles", installed, sb);		
			printBundles("Updated Bundles", updated, sb);
			switch(errors) {
			case 0: break;
			case 1: sb.append("One Error\n"); break;
			default: sb.append(errors); sb.append(" Errors\n");
			}
			activator.message(sb.toString());
		}
	}

	private void printBundles(String msg, List<Bundle> list, StringBuilder sb) {
		if ( list.isEmpty() )
			return;
		
		sb.append(msg);
		sb.append('\n');
		for ( Bundle  b : list ) {
			String version = (String) b.getHeaders().get("Bundle-Version");
			if ( version == null )
				version = "0.0.0";
			
			String name = b.getSymbolicName();
			if ( name == null )
				name = b.getLocation();
			
			sb.append("  ");
			sb.append(name);
			for ( int p = name.length(); p<20; p++ )
				sb.append(" ");
			sb.append("- ");
			sb.append(version);
			sb.append("\n");
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
				result[n++] = (IFile) i.next();
			}
			return result;
		}
		return null;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

}
