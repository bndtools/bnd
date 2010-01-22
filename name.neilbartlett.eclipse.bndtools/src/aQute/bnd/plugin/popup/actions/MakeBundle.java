package aQute.bnd.plugin.popup.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.eclipse.EclipseClasspath;

public class MakeBundle implements IObjectActionDelegate {
    IFile[] locations;
	IWorkbenchPart targetPart;

    public MakeBundle() {
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        Plugin plugin = Plugin.getDefault();

        try {
            if (locations != null) {
                for (int i = 0; i < locations.length; i++) {
                    try {
                        File mf = locations[i].getLocation().toFile();
                        if (mf.getName().equals(Project.BNDFILE)) {
                            Project project = Workspace.getProject(mf
                                    .getParentFile());
                            File[] files = project.build();
                            String target = "";
                            if ( files != null ) {
                                for ( File f : files ) {
                                    target += f + " ";
                                    
                                }
                            }
                            plugin.report(true, true, project, "Building "
                                    + project, "Created files " + target );
                        } else {
                            Builder builder = setBuilder(plugin,
                                    locations[i].getProject(), mf);

                            File cwd = mf.getAbsoluteFile().getParentFile();
                            File target;

                            builder.build();
                            String name = builder.getBsn() + ".jar";

                            Jar jar = builder.getJar();

                            String path = builder.getProperty("-output");
                            if (path == null) {
                                target = new File(cwd, name);
                            } else {
                                target = new File(path);
                                if (!target.isAbsolute())
                                    target = new File(cwd, path);
                                if (target.isDirectory()) {
                                    target = new File(target, name);
                                }
                            }

                            target.delete();
                            if (builder.getErrors().size() > 0) {
                                plugin.error(builder.getErrors());
                            } else {
                                jar.write(target);

                                if (builder.getWarnings().size() > 0) {
                                    plugin.warning(builder.getWarnings());
                                } else {
                                    String p = target.getPath();
                                    if (p.startsWith(cwd.getAbsolutePath()))
                                        p = p
                                                .substring(cwd
                                                        .getAbsolutePath()
                                                        .length() + 1);
                                    String msg = "Saved as " + p;
                                    showOnStatusLine(msg);
                                }
                            }
                            builder.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        plugin.error("While generating JAR " + locations[i],
                                e);
                    }
                    locations[i].getParent().refreshLocal(1, null);
                }
            }
        } catch (Exception e) {
            plugin.error("Error in bnd", e);
        }
    }

    static public Builder setBuilder(Plugin plugin, IProject project,
            File mf) throws Exception, IOException, FileNotFoundException {
        Builder builder = new Builder();
        //builder.setPedantic(activator.isPedantic() || activator.isDebugging());

        // TODO of course we should get the classpath from
        // inside API ...
        File p = project.getLocation().toFile();

        // TODO for now we ignore the workspace and use the
        // project parent directory

        EclipseClasspath ecp = new EclipseClasspath(builder, p.getParentFile(),
                p);

        builder.setClasspath((File[]) ecp.getClasspath().toArray(new File[0]));
        builder
                .setSourcepath((File[]) ecp.getSourcepath()
                        .toArray(new File[0]));
        builder.setProperties(mf);
        return builder;
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
		this.targetPart = targetPart;
    }
    
    void showOnStatusLine(String message) {
    	IWorkbenchPartSite site = targetPart.getSite();
    	if(site instanceof IViewSite) {
    		IStatusLineManager statusLineManager = ((IViewSite) site).getActionBars().getStatusLineManager();
    		statusLineManager.setMessage(message);
    	} else if(site instanceof IEditorSite) {
    		IStatusLineManager statusLineManager = ((IEditorSite) site).getActionBars().getStatusLineManager();
    		statusLineManager.setMessage(message);
    	} else {
    		Plugin.getDefault().message(message);
    	}
    }

}
