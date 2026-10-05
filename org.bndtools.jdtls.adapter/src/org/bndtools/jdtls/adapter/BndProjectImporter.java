package org.bndtools.jdtls.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bndtools.jdtls.BndJdtLsBridge;
import org.bndtools.jdtls.BndWorkspaceDetector;
import org.bndtools.jdtls.dto.BndProjectInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ls.core.internal.AbstractProjectImporter;
import org.eclipse.jdt.ls.core.internal.managers.IBuildSupport;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager.CHANGE_TYPE;

import aQute.bnd.build.Workspace;

public class BndProjectImporter extends AbstractProjectImporter {
    public static final String CONTAINER_ID = "org.bndtools.jdtls.BND_CONTAINER";
    private static final String PLUGIN_ID = "org.bndtools.jdtls.adapter";
    private static final BndJdtLsBridge BRIDGE = new BndJdtLsBridge();

    @Override
    public boolean applies(IProgressMonitor monitor) {
        return new BndWorkspaceDetector().findWorkspaceRoot(rootFolder).isPresent();
    }

    @Override
    public void importToWorkspace(IProgressMonitor monitor) throws CoreException {
        try {
            File root = new BndWorkspaceDetector().findWorkspaceRoot(rootFolder).orElseThrow();
            IProject configuration = ResourcesPlugin.getWorkspace().getRoot()
                .getProject("bnd.cnf." + Integer.toHexString(root.getCanonicalPath().hashCode()));
            if (!configuration.exists()) {
                IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(configuration.getName());
                description.setLocation(Path.fromOSString(new File(root, "cnf").getAbsolutePath()));
                configuration.create(description, monitor);
            }
            if (!configuration.isOpen()) configuration.open(monitor);
            for (BndProjectInfo info : BRIDGE.getWorkspaceProjects(rootFolder)) {
                if (monitor.isCanceled()) return;
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(info.getName());
                if (!project.exists()) {
                    IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(info.getName());
                    description.setLocation(Path.fromOSString(info.getBasePath()));
                    project.create(description, monitor);
                }
                if (!project.isOpen()) project.open(monitor);
                if (!project.getLocation().toFile().getCanonicalFile().equals(new File(info.getBasePath()).getCanonicalFile())) {
                    throw new IllegalStateException("Another project already uses name " + info.getName());
                }
                configure(project, info, monitor);
            }
        } catch (Exception exception) {
            throw failure(exception);
        }
    }

    @Override
    public void reset() {}

    private static void configure(IProject project, BndProjectInfo info, IProgressMonitor monitor) throws Exception {
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID });
        var builder = description.newCommand();
        builder.setBuilderName(JavaCore.BUILDER_ID);
        description.setBuildSpec(new org.eclipse.core.resources.ICommand[] { builder });
        project.setDescription(description, monitor);
        IJavaProject javaProject = JavaCore.create(project);
        List<IClasspathEntry> entries = new ArrayList<>();
        for (String source : info.getSourcePaths()) {
            if (new File(source).isDirectory()) entries.add(JavaCore.newSourceEntry(local(project, source),
                new IPath[0], new IPath[0], local(project, info.getOutputPath())));
        }
        for (String source : info.getTestSourcePaths()) {
            if (new File(source).isDirectory()) entries.add(JavaCore.newSourceEntry(local(project, source),
                new IPath[0], new IPath[0], local(project, info.getTestOutputPath()),
                new IClasspathAttribute[] { JavaCore.newClasspathAttribute("test", "true") }));
        }
        File base = new File(info.getBasePath());
        Workspace workspace = Workspace.getWorkspace(new BndWorkspaceDetector().findWorkspaceRoot(base).orElseThrow());
        var model = workspace.getProject(info.getName());
        String target = model.getProperty("javac.target", "17");
        String runee = "8".equals(target) ? "1.8" : target;
        entries.add(JavaCore.newContainerEntry(new Path(JavaRuntime.JRE_CONTAINER)
            .append("org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType").append("JavaSE-" + runee)));
        entries.add(JavaCore.newContainerEntry(new Path(CONTAINER_ID)));
        for (String file : info.getTestPath()) {
            if (!info.getBuildPath().contains(file)) entries.add(library(file, true));
        }
        javaProject.setRawClasspath(entries.toArray(IClasspathEntry[]::new), local(project, info.getOutputPath()), monitor);
        JavaCore.setClasspathContainer(new Path(CONTAINER_ID), new IJavaProject[] { javaProject },
            new IClasspathContainer[] { container(info, new Path(CONTAINER_ID)) }, monitor);
        javaProject.setOption(JavaCore.COMPILER_SOURCE, model.getProperty("javac.source", runee));
        javaProject.setOption(JavaCore.COMPILER_COMPLIANCE, runee);
        javaProject.setOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, runee);
    }

    private static IPath local(IProject project, String path) {
        IPath absolute = Path.fromOSString(path);
        if (!project.getLocation().isPrefixOf(absolute)) {
            throw new IllegalArgumentException("Source/output outside project is not supported: " + path);
        }
        return project.getFullPath().append(absolute.makeRelativeTo(project.getLocation()));
    }

    private static IClasspathContainer container(BndProjectInfo info, IPath path) {
        List<IClasspathEntry> entries = new ArrayList<>();
        for (String file : info.getBuildPath()) entries.add(library(file, false));
        return new IClasspathContainer() {
            public IClasspathEntry[] getClasspathEntries() { return entries.toArray(IClasspathEntry[]::new); }
            public String getDescription() { return "bnd build dependencies"; }
            public int getKind() { return K_APPLICATION; }
            public IPath getPath() { return path; }
        };
    }

    private static IClasspathEntry library(String path, boolean test) {
        File file = new File(path);
        if (!file.exists()) throw new IllegalArgumentException("Missing bnd dependency: " + path);
        IClasspathAttribute[] attributes = test
            ? new IClasspathAttribute[] { JavaCore.newClasspathAttribute("test", "true") } : new IClasspathAttribute[0];
        return JavaCore.newLibraryEntry(Path.fromOSString(path), null, null, null, attributes, false);
    }

    private static CoreException failure(Exception exception) {
        return new CoreException(new Status(Status.ERROR, PLUGIN_ID, exception.getMessage(), exception));
    }

    public static class ContainerInitializer extends ClasspathContainerInitializer {
        @Override
        public void initialize(IPath path, IJavaProject project) throws CoreException {
            try {
                BndProjectInfo info = BRIDGE.getProjectInfo(project.getProject().getLocation().toFile()).orElseThrow();
                JavaCore.setClasspathContainer(path, new IJavaProject[] { project },
                    new IClasspathContainer[] { container(info, path) }, null);
            } catch (Exception exception) {
                throw failure(exception);
            }
        }
    }

    public static class BuildSupport implements IBuildSupport {
        @Override
        public boolean applies(IProject project) {
            if (project.getLocation() == null || !project.isOpen()) return false;
            if (project.getName().startsWith("bnd.cnf.")) return true;
            try {
                return Arrays.stream(JavaCore.create(project).getRawClasspath())
                    .anyMatch(entry -> entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
                        && CONTAINER_ID.equals(entry.getPath().segment(0)));
            } catch (CoreException exception) {
                return false;
            }
        }

        @Override
        public boolean isBuildFile(IResource resource) {
            return resource.getType() == IResource.FILE
                && (resource.getName().endsWith(".bnd") || resource.getName().endsWith(".mvn"));
        }

        @Override
        public void update(IProject project, boolean force, IProgressMonitor monitor) throws CoreException {
            try {
                BRIDGE.refreshWorkspace(project.getLocation().toFile());
                File root = new BndWorkspaceDetector().findWorkspaceRoot(project.getLocation().toFile()).orElseThrow();
                for (var model : Workspace.getWorkspace(root).getAllProjects()) model.refresh();
                for (BndProjectInfo info : BRIDGE.getWorkspaceProjects(project.getLocation().toFile())) {
                    IProject target = ResourcesPlugin.getWorkspace().getRoot().getProject(info.getName());
                    if (target.isOpen() && applies(target)) configure(target, info, monitor);
                }
            } catch (Exception exception) {
                throw failure(exception);
            }
        }

        @Override
        public boolean fileChanged(IResource resource, CHANGE_TYPE change, IProgressMonitor monitor) throws CoreException {
            if (isBuildFile(resource)) {
                update(resource.getProject(), true, monitor);
                return true;
            }
            return false;
        }

        @Override
        public List<String> getWatchPatterns() { return List.of("**/*.bnd", "**/*.mvn"); }

        @Override
        public String buildToolName() { return "bnd"; }
    }
}
