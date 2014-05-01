package org.bndtools.utils.jar;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class JarUtils {

    public static Manifest loadJarManifest(InputStream stream) throws IOException {
        JarInputStream jarInputStream = null;
        try {
            jarInputStream = new JarInputStream(stream);
            return jarInputStream.getManifest();
        } finally {
            if (jarInputStream != null) {
                try {
                    jarInputStream.close();
                } catch (IOException e) {
                    /* swallow */
                }
            }
        }
    }
}
