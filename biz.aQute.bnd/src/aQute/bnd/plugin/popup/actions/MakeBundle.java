package aQute.bnd.plugin.popup.actions;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.eclipse.*;

public class MakeBundle implements IObjectActionDelegate {
    IFile[] locations;

    public MakeBundle() {
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        Activator activator = Activator.getDefault();

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
                            activator.report(true, true, project, "Building "
                                    + project, "Created files " + target );
                        } else {
                            Builder builder = setBuilder(activator,
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
                                activator.error(builder.getErrors());
                            } else {
                                jar.write(target);

                                File copy = activator.getCopy();
                                if (copy != null) {
                                    copy = new File(copy, target.getName());
                                    jar.write(copy);
                                }
                                if (builder.getWarnings().size() > 0) {
                                    activator.warning(builder.getWarnings());
                                } else {
                                    if (activator.getReportDone()) {
                                        String p = target.getPath();
                                        if (p.startsWith(cwd.getAbsolutePath()))
                                            p = p
                                                    .substring(cwd
                                                            .getAbsolutePath()
                                                            .length() + 1);
                                        String msg = "Saved as " + p;
                                        if (copy != null)
                                            msg += " and copied to " + copy;
                                        activator.message(msg);
                                    }
                                }
                            }
                            builder.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        activator.error("While generating JAR " + locations[i],
                                e);
                    }
                    locations[i].getParent().refreshLocal(1, null);
                }
            }
        } catch (Exception e) {
            activator.error("Error in bnd", e);
        }
    }

    static public Builder setBuilder(Activator activator, IProject project,
            File mf) throws Exception, IOException, FileNotFoundException {
        Builder builder = new Builder();
        builder.setPedantic(activator.isPedantic() || activator.isDebugging());

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
    }

}
