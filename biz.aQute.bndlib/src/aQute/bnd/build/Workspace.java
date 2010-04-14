package aQute.bnd.build;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

import aQute.bnd.service.*;
import aQute.bnd.service.action.*;
import aQute.lib.osgi.*;

public class Workspace extends Processor {
    public static final String                 BUILDFILE = "build.bnd";
    public static final String                 CNFDIR    = "cnf";

    static Map<File, WeakReference<Workspace>> cache     = newHashMap();
    final Map<String, Project>                 models    = newHashMap();
    final Map<String, Action>                  commands  = newMap();

    /**
     * This static method finds the workspace and creates a project (or returns
     * an existing project)
     * 
     * @param projectDir
     * @return
     */
    public static Project getProject(File projectDir) throws Exception {
        projectDir = projectDir.getAbsoluteFile();
        assert projectDir.isDirectory();

        Workspace ws = getWorkspace(projectDir.getParentFile());
        return ws.getProject(projectDir.getName());
    }

    public static Workspace getWorkspace(File workspaceDir) throws Exception {
        workspaceDir = workspaceDir.getAbsoluteFile();
        synchronized (cache) {
            WeakReference<Workspace> wsr = cache.get(workspaceDir);
            Workspace ws;
            if (wsr == null || (ws = wsr.get()) == null) {
                ws = new Workspace(workspaceDir);
                cache.put(workspaceDir, new WeakReference<Workspace>(ws));
            }
            return ws;
        }
    }

    public Workspace(File dir) throws Exception {
        dir = dir.getAbsoluteFile();
        dir.mkdirs();
        assert dir.isDirectory();

        File buildDir = new File(dir, CNFDIR).getAbsoluteFile();
        File buildFile = new File(buildDir, BUILDFILE).getAbsoluteFile();
        if (!buildFile.isFile())
            warning("No Build File in " + dir);
        setProperties(buildFile, dir);
    }

    public Project getProject(String bsn) throws Exception {
        synchronized (models) {
            Project project = models.get(bsn);
            if (project != null)
                return project;

            File projectDir = getFile(bsn);
            project = new Project(this, projectDir);
            models.put(bsn, project);
            return project;
        }
    }

    public boolean isPresent(String name) {
        return models.containsKey(name);
    }

    public Collection<Project> getCurrentProjects() {
        return models.values();
    }

    public boolean refresh() {
        if (super.refresh()) {
            for (Project project : getCurrentProjects()) {
                project.propertiesChanged();
            }
            return true;
        }
        return false;
    }

    public String _workspace(String args[]) {
        return getBase().getAbsolutePath();
    }

    public void addCommand(String menu, Action action) {
        commands.put(menu, action);
    }

    public void removeCommand(String menu) {
        commands.remove(menu);
    }

    public void fillActions(Map<String, Action> all) {
        all.putAll(commands);
    }

    public Collection<Project> getAllProjects() throws Exception {
        List<Project> projects = new ArrayList<Project>();
        for (File file : getBase().listFiles()) {
            if (new File(file, Project.BNDFILE).isFile())
                projects.add(getProject(file));
        }
        return projects;
    }

    /**
     * Inform any listeners that we changed a file (created/deleted/changed).
     * 
     * @param f The changed file
     */
    public void changedFile(File f) {
        List<BndListener> listeners = getPlugins(BndListener.class);
        for (BndListener l : listeners)
            try {
                l.changed(f);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

}
