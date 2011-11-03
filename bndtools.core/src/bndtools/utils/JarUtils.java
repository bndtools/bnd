package bndtools.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import aQute.lib.io.IO;

public class JarUtils {

    public static Manifest loadJarManifest(InputStream stream) throws IOException {
        try {
            JarInputStream jarInputStream = new JarInputStream(stream);
            return jarInputStream.getManifest();
        } finally {
            IO.close(stream);
        }
    }
}
