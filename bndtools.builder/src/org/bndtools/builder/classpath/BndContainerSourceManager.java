package org.bndtools.builder.classpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.bndtools.builder.NewBuilder;
import org.bndtools.builder.Plugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import bndtools.central.Central;
import bndtools.central.RepositoryUtils;

public class BndContainerSourceManager {

    private static final String PROPERTY_SRC_ROOT = ".srcRoot"; //$NON-NLS-1$

    private static final String PROPERTY_SRC_PATH = ".srcPath"; //$NON-NLS-1$

    /**
     * Persist the attached sources for given {@link IClasspathEntry} instances.
     */
    public static void saveAttachedSources(final IJavaProject project, final List<IClasspathEntry> classpathEntries) throws CoreException {
        final Properties props = new Properties();

        // Construct the Properties that represent the source attachment(s)
        for (final IClasspathEntry entry : classpathEntries) {
            if (IClasspathEntry.CPE_LIBRARY != entry.getEntryKind()) {
                continue;
            }
            final String path = entry.getPath().toPortableString();
            if (entry.getSourceAttachmentPath() != null) {
                props.put(path + PROPERTY_SRC_PATH, entry.getSourceAttachmentPath().toPortableString());
            }
            if (entry.getSourceAttachmentRootPath() != null) {
                props.put(path + PROPERTY_SRC_ROOT, entry.getSourceAttachmentRootPath().toPortableString());
            }
        }

        // Write the properties to a persistent storage area
        final File propertiesFile = getSourceAttachmentPropertiesFile(project.getProject());
        if (props.isEmpty()) {
            IO.delete(propertiesFile);
        } else {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(propertiesFile);
                props.store(out, new Date().toString());
            } catch (final IOException e) {
                throw new CoreException(new Status(Status.ERROR, NewBuilder.PLUGIN_ID, "Failure to write container source attachments", e));
            } finally {
                IO.close(out);
            }
        }
    }

    /**
     * Return (a potentially modified) list of {@link IClasspathEntry} instances that will have any previously persisted
     * attached sources added.
     */
    public static List<IClasspathEntry> loadAttachedSources(final IJavaProject project, final List<IClasspathEntry> classPathEntries) throws CoreException {
        final Properties props = loadSourceAttachmentProperties(project.getProject());

        final List<IClasspathEntry> configuredClassPathEntries = new ArrayList<IClasspathEntry>(classPathEntries.size());

        for (final IClasspathEntry entry : classPathEntries) {
            if (entry.getEntryKind() != IClasspathEntry.CPE_LIBRARY || entry.getSourceAttachmentPath() != null) {
                configuredClassPathEntries.add(entry);
                continue;
            }

            final String key = entry.getPath().toPortableString();

            IPath srcPath = null;
            IPath srcRoot = null;

            // Retrieve the saved source attachment information
            if (props != null && props.containsKey(key + PROPERTY_SRC_PATH)) {
                srcPath = Path.fromPortableString((String) props.get(key + PROPERTY_SRC_PATH));
                if (props.containsKey(key + PROPERTY_SRC_ROOT)) {
                    srcRoot = Path.fromPortableString((String) props.get(key + PROPERTY_SRC_ROOT));
                }
            } else {
                // If there is no saved source attachment, then try and find a source bundle
                File sourceBundle = getSourceBundle(entry.getPath());
                if (sourceBundle != null) {
                    srcPath = new Path(sourceBundle.getAbsolutePath());
                }
            }

            if (srcPath != null || srcRoot != null) {
                configuredClassPathEntries.add(JavaCore.newLibraryEntry(entry.getPath(), srcPath, srcRoot, entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported()));
            } else {
                configuredClassPathEntries.add(entry);
            }
        }

        return configuredClassPathEntries;
    }

    private static File getSourceBundle(IPath path) {
        Workspace bndWorkspace;

        try {
            bndWorkspace = Central.getWorkspace();
            if (bndWorkspace == null) {
                return null;
            }
        } catch (final Exception e) {
            return null;
        }

        IPath bundlePath = path;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IResource resource = root.findMember(path);
        if (resource != null) {
            bundlePath = resource.getLocation();
        }

        JarInputStream jarStream = null;
        try {
            jarStream = new JarInputStream(new FileInputStream(bundlePath.toFile()), false);
            Manifest mf = jarStream.getManifest();
            if (mf == null) {
                return null;
            }
            String bsn = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
            String version = mf.getMainAttributes().getValue(Constants.BUNDLE_VERSION);

            for (RepositoryPlugin repo : RepositoryUtils.listRepositories(true)) {
                if (repo == null) {
                    continue;
                }
                if (repo instanceof WorkspaceRepository) {
                    continue;
                }
                File sourceBundle = repo.get(bsn + ".source", new Version(version), null);
                if (sourceBundle != null) {
                    return sourceBundle;
                }
            }
        } catch (final Exception e) {
            // Ignore, something went wrong, or we could not find the source bundle
        } finally {
            IO.close(jarStream);
        }

        return null;
    }

    private static Properties loadSourceAttachmentProperties(final IProject project) throws CoreException {
        final Properties props = new Properties();

        final File propertiesFile = getSourceAttachmentPropertiesFile(project);
        if (propertiesFile.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(propertiesFile);
                props.load(in);
            } catch (final IOException e) {
                throw new CoreException(new Status(Status.ERROR, NewBuilder.PLUGIN_ID, "Failure to read container source attachments", e));
            } finally {
                IO.close(in);
            }
        }

        return props;
    }

	private static File getSourceAttachmentPropertiesFile(final IProject project) {
		return new File(Plugin.getInstance().getStateLocation().toFile(), project.getName() + ".sources"); //$NON-NLS-1$
	}

}
