package org.bndtools.utils.jar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class JarUtils {

    @SuppressWarnings("resource")
    public static Manifest loadJarManifest(File file) throws IOException {
        InputStream in = null;
        try {
            if (file.isDirectory()) { // expanded JAR on file system
                in = new FileInputStream(new File(file, "META-INF/MANIFEST.MF"));
                return new Manifest(in);
            }
            JarInputStream jarInputStream;
            in = jarInputStream = new JarInputStream(new FileInputStream(file));
            return jarInputStream.getManifest();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    /* swallow */
                }
            }
        }
    }
}
