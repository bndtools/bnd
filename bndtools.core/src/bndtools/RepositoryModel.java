package bndtools;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Jar;

public class RepositoryModel {

    private final HashMap<String, Map<File, BundleInfo>> namesToBundlesToClazzes = new HashMap<String, Map<File, BundleInfo>>();

    private void insertBundleInfo(BundleInfo info) {
        for (Clazz clazz : info.clazzes) {
            Clazz.getShortName(clazz.getFQN());
        }
    }

    /**
     *
     * @param repo
     * @param file
     * @param monitor
     *            the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call done() on the
     *            given monitor. Accepts null, indicating that no progress
     *            should be reported and that the operation cannot be cancelled.
     *            * @throws IOException
     * @throws CoreException
     */
    public void updateRepositoryBundle(RepositoryPlugin repo, File file, IProgressMonitor monitor) throws IOException, CoreException {
        SubMonitor progress = SubMonitor.convert(monitor);

        Collection<Clazz> clazzes = analyseBundleFile(file);
        int size = clazzes != null ? clazzes.size() : 0;
        System.out.println(MessageFormat.format("Found {0} public classes in bundle {1}.", size, file.getPath()));
    }

    Collection<Clazz> analyseBundleFile(File file) throws IOException, CoreException {
        Builder builder = new Builder();
        Jar jar = new Jar(file);
        builder.setJar(jar);

        try {
            builder.analyze();
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to analyse bundle \"{0}\".", file.getPath()), e));
        } finally {
            jar.close();
        }

        String bsn = builder.getBsn();
        String version = builder.getVersion();

        Collection<Clazz> clazzes = null;
        try {
            clazzes = builder.getClasses("classes", "PUBLIC");
            if(clazzes != null) {
                BundleInfo bundleInfo = new BundleInfo(bsn, version, file, clazzes);
                insertBundleInfo(bundleInfo);
            }
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to analyse public classes in bundle \"{0}\".", file.getPath()), e));
        }
        return clazzes;
    }
}

class BundleInfo {
    final String bsn;
    final String version;
    final File file;
    final Collection<Clazz> clazzes;

    public BundleInfo(String bsn, String version, File file, Collection<Clazz> clazzes) {
        this.bsn = bsn; this.version = version; this.file = file; this.clazzes = clazzes;
    }
}