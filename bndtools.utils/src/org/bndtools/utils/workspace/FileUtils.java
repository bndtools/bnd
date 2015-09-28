package org.bndtools.utils.workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.bndtools.utils.osgi.BundleUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

public class FileUtils {
    public static IDocument readFully(IFile file) throws CoreException, IOException {
        if (file.exists()) {
            InputStream stream = file.getContents();
            byte[] bytes = readFully(stream);

            String string = new String(bytes, file.getCharset());
            return new Document(string);
        }
        return null;
    }

    public static void recurseCreate(IContainer container, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);
        if (container == null || container.exists())
            return;

        recurseCreate(container.getParent(), progress.newChild(1, SubMonitor.SUPPRESS_NONE));

        if (container instanceof IFolder)
            ((IFolder) container).create(false, true, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
        else
            throw new CoreException(new Status(IStatus.ERROR, BundleUtils.getBundleSymbolicName(FileUtils.class), 0, "Cannot create new projects or workspace roots automatically.", null));
    }

    public static byte[] readFully(InputStream stream) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            final byte[] buffer = new byte[1024];
            while (true) {
                int read = stream.read(buffer, 0, 1024);
                if (read == -1)
                    break;
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            stream.close();
        }
    }

    public static void writeFully(IDocument document, IFile file, boolean createIfAbsent) throws CoreException {
        writeFully(document.get(), file, createIfAbsent);
    }

    public static void writeFully(String text, IFile file, boolean createIfAbsent) throws CoreException {
        ByteArrayInputStream inputStream;
        try {
            String charset = file.getCharset(true);
            if (charset == null) {
                charset = Charset.defaultCharset().name();
            }
            inputStream = new ByteArrayInputStream(text.getBytes(charset));
        } catch (UnsupportedEncodingException e) {
            return;
        }
        if (file.exists()) {
            file.setContents(inputStream, false, true, null);
        } else {
            if (createIfAbsent)
                file.create(inputStream, false, null);
            else
                throw new CoreException(new Status(IStatus.ERROR, BundleUtils.getBundleSymbolicName(FileUtils.class), 0, "File does not exist: " + file.getFullPath().toString(), null));
        }
    }

    public static boolean isAncestor(File dir, File child) {
        if (child == null)
            return false;
        File c = child.getAbsoluteFile();
        if (c.equals(dir))
            return true;
        return isAncestor(dir, c.getParentFile());
    }

    public static IResource toProjectResource(IProject project, File file) {
        if (file == null) {
            return null;
        }

        String projectPath = project.getLocation().toFile().getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (!filePath.startsWith(projectPath)) {
            return null;
        }

        return project.getFolder(filePath.substring(projectPath.length()));
    }

    public static IResource toWorkspaceResource(File file) {
        IPath path = new Path(file.toString());

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IWorkspaceRoot workspaceRoot = workspace.getRoot();
        IPath workspacePath = workspaceRoot.getLocation();

        if (workspacePath.isPrefixOf(path)) {
            final IPath relativePath = path.removeFirstSegments(workspacePath.segmentCount());
            IResource resource;
            if (file.isDirectory()) {
                resource = workspaceRoot.getFolder(relativePath);
            } else {
                resource = workspaceRoot.getFile(relativePath);
            }
            return resource;
        }
        return null;
    }

    public static IFile[] getWorkspaceFiles(File javaFile) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        return root.findFilesForLocationURI(javaFile.toURI());
    }

    public static File toFile(IWorkspaceRoot root, IPath path) {
        IFile wsfile;
        IFolder wsfolder;

        if ((wsfile = root.getFile(path)).exists()) {
            IPath location = wsfile.getLocation();
            if (location != null)
                return location.toFile();
        }

        if ((wsfolder = root.getFolder(path)).exists()) {
            IPath location = wsfolder.getLocation();
            if (location != null)
                return location.toFile();
        }

        return path.toFile();
    }

    public static void dumpResourceDelta(IResourceDelta delta, PrintStream out) {
        dumpResourceDelta(delta, out, "");
    }

    private static void dumpResourceDelta(IResourceDelta delta, PrintStream out, String indent) {
        out.println(String.format("%s%s: kind=%h, flags=%h", indent, delta.getFullPath(), delta.getKind(), delta.getFlags()));
        IResourceDelta[] children = delta.getAffectedChildren();
        for (IResourceDelta child : children) {
            dumpResourceDelta(child, out, indent + "   ");
        }
    }

    public static void mkdirs(IContainer container, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);

        if (container.exists())
            return;

        IContainer parent = container.getParent();
        if (parent != null)
            mkdirs(parent, progress.newChild(1));

        if (container.getType() == IResource.FOLDER) {
            IFolder folder = (IFolder) container;
            folder.create(false, true, progress.newChild(1));
        } else {
            throw new CoreException(new Status(IStatus.ERROR, "bndtools.utils", 0, "Can only create plain Folder parent containers.", null));
        }
    }

}