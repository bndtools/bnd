package aQute.bnd.signing;

import java.io.*;
import java.util.*;

import aQute.bnd.service.*;
import aQute.lib.osgi.*;
import aQute.libg.reporter.*;

/**
 * Sign the jar file.
 * 
 * -sign : <alias> [ ';' 'password:=' <password> ] [ ';' 'keystore:=' <keystore> ] [
 * ';' 'sign-password:=' <pw> ] ( ',' ... )*
 * 
 * @author aqute
 * 
 */
public class JartoolSigner implements Plugin, SignerPlugin {
    String keystore;
    String storetype;
    String path = "jarsigner";
    String storepass;
    String keypass;
    String sigFile;

    public void setProperties(Map<String, String> map) {
        if (map.containsKey("keystore"))
            this.keystore = map.get("keystore");
        if (map.containsKey("storetype"))
            this.storetype = map.get("storetype");
        if (map.containsKey("storepass"))
            this.storepass = map.get("storepass");
        if (map.containsKey("keypass"))
            this.keypass = map.get("keypass");
        if (map.containsKey("path"))
            this.path = map.get("path");
        if (map.containsKey("sigFile"))
            this.sigFile = map.get("sigFile");

    }

    public void setReporter(Reporter processor) {
    }

    public void sign(Builder builder, String alias) throws Exception {
        Jar jar = builder.getJar();
        File tmp = File.createTempFile("signdjar", ".jar");
        tmp.deleteOnExit();

        jar.write(tmp);

        StringBuilder sb = new StringBuilder();
        sb.append(path);
        if (keystore != null) {
            sb.append(" -keystore ");
            sb.append(keystore);
        }

        if (storetype != null) {
            sb.append(" -storetype ");
            sb.append(storetype);
        }

        if (keypass != null) {
            sb.append(" -keypass ");
            sb.append(keypass);
        }

        if (storepass != null) {
            sb.append(" -storepass ");
            sb.append(storepass);
        }

        if (sigFile != null) {
            sb.append(" -sigFile ");
            sb.append(sigFile);
        }

        sb.append(" ");
        sb.append(tmp.getAbsolutePath());
        sb.append(" ");
        sb.append(alias);

        String cmd = sb.toString();

        builder.trace(cmd);
        Process process = Runtime.getRuntime().exec(cmd);
        StringBuffer sbin = collect( process.getInputStream());
        StringBuffer sberr = collect( process.getErrorStream());        
        process.waitFor();

        if (process.exitValue() != 0) {
            builder.error("Signing Jar out: %s\nerr: %s", sbin, sberr);
        } else {
            builder.trace("Signing Jar out: %s \nerr: %s", sbin, sberr);
        }

        Jar signed = new Jar(tmp);
        builder.addClose(signed);

        Map<String, Resource> dir = signed.getDirectories().get("META-INF");
        for (String path : dir.keySet()) {
            if (path.matches(".*\\.(DSA|RSA|SF|MF)$")) {
                jar.putResource(path, dir.get(path));
            }
        }
        jar.setDoNotTouchManifest();
    }

    StringBuffer collect(final InputStream in) throws Exception {
        final StringBuffer sb = new StringBuffer();
        
        Thread tin = new Thread() {
            public void run() {
                try {
                    BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
                    String line = rdr.readLine();
                    while (line != null) {
                        sb.append(line);
                        line = rdr.readLine();
                    }
                    rdr.close();
                    in.close();
                } catch (Exception e) {
                    // Ignore any exceptions
                }
            }
        };
        tin.start();
        return sb;
    }
}
