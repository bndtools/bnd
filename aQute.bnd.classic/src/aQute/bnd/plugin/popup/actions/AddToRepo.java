package aQute.bnd.plugin.popup.actions;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;
import aQute.bnd.plugin.popup.actions.repo.*;
import aQute.bnd.service.*;
import aQute.lib.osgi.*;

public class AddToRepo implements IObjectActionDelegate {
    IFile[] locations;

    public AddToRepo() {
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        try {
            if (locations != null) {
                for (int i = 0; i < locations.length; i++) {
                    File mf = locations[i].getLocation().toFile();
                    try {

                        Jar jar = new Jar(mf.getName(), mf);

                        try {
                            Verifier verifier = new Verifier(jar);
                            verifier.verify();
                            if (verifier.getErrors().size()
                                    + verifier.getWarnings().size() > 0) {
                                List<String> info = new ArrayList<String>(
                                        verifier.getErrors());
                                info.addAll(verifier.getWarnings());
                                Activator.getDefault().error(info);
                            } else {
                                Workspace ws = Central.getWorkspace();
                                List<RepositoryPlugin> repos = ws
                                        .getPlugins(RepositoryPlugin.class);
                                RepoDialog d = new RepoDialog(null, jar, repos);
                                d.setBlockOnOpen(true);
                                if (d.open() == 0)
                                    try {
                                        RepositoryPlugin repo = d
                                                .getRepository();
                                        File f = repo.put(jar);
                                  //      Central.refresh(Path.fromOSString(f
                                  //              .getAbsolutePath()));
                                    } finally {
                                        d.close();
                                    }
                            }
                        } finally {
                            jar.close();
                        }

                    } catch (Exception e) {
                        Activator.getDefault().error(
                                "While verifying JAR " + locations[i], e);
                    }
                    locations[i].getParent().refreshLocal(1, null);
                }
            }
        } catch (Exception e) {
            Activator.getDefault().error("Could not start verification", e);
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
