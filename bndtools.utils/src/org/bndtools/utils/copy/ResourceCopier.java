package org.bndtools.utils.copy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

public class ResourceCopier {
    public static IFile copy(URL url, IFile dst, Map<String,String> replaceRegularExpressions, IProgressMonitor monitor) throws IOException, CoreException {
        InputStream is = null;
        try {
            SubMonitor progress = SubMonitor.convert(monitor, 2);

            if (url.getPath().endsWith("/")) {
                File file = dst.getProjectRelativePath().toFile();

                if (file.isDirectory())
                    return dst; // already done

                if (file.isFile())
                    throw new IllegalArgumentException("Expected no file or a directory, but was a file: " + file);

                file.mkdirs();
                return dst; // already done
            }

            ResourceReplacer replacer = null;
            if ((replaceRegularExpressions == null) || replaceRegularExpressions.isEmpty()) {
                is = url.openStream();
            } else {
                replacer = new ResourceReplacer(replaceRegularExpressions, url);
                replacer.start();
                is = replacer.getStream();
            }

            if (dst.exists()) {
                dst.setContents(is, false, true, progress.newChild(2, SubMonitor.SUPPRESS_NONE));
            } else {
                FileUtils.recurseCreate(dst.getParent(), progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                dst.create(is, false, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
            }

            if (replacer != null) {
                try {
                    replacer.join();
                } catch (InterruptedException e) {
                    /* swallow */
                }
                if (replacer.getResult() != null) {
                    throw replacer.getResult();
                }
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    /* swallow */
                }
            }
        }

        return dst;
    }
}
