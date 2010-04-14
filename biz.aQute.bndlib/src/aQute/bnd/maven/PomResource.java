package aQute.bnd.maven;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.lib.osgi.*;

public class PomResource extends WriteResource {
    final Manifest       manifest;
    final static Pattern NAME_URL = Pattern.compile("(.*)(http://.*)");

    public PomResource(Manifest manifest) {
        this.manifest = manifest;
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        PrintStream ps = new PrintStream(out);

        String name = manifest.getMainAttributes().getValue(
                Analyzer.BUNDLE_NAME);

        String description = manifest.getMainAttributes().getValue(
                Constants.BUNDLE_DESCRIPTION);
        String docUrl = manifest.getMainAttributes().getValue(
                Constants.BUNDLE_DOCURL);
        String version = manifest.getMainAttributes().getValue(
                Constants.BUNDLE_VERSION);
        String bundleVendor = manifest.getMainAttributes().getValue(
                Constants.BUNDLE_VENDOR);

        String bsn = manifest.getMainAttributes().getValue(
                Constants.BUNDLE_SYMBOLICNAME);
        String licenses = manifest.getMainAttributes().getValue(
                Constants.BUNDLE_LICENSE);

        if (bsn == null) {
            throw new RuntimeException("Cannot create POM unless bsn is set");
        }

        bsn = bsn.trim();
        int n = bsn.lastIndexOf('.');
        if (n <= 0)
            throw new RuntimeException(
                    "Can not create POM unless Bundle-SymbolicName contains a . to separate group and  artifact id");

        if ( version == null )
            version = "0";
        
        String groupId = bsn.substring(0, n);
        String artifactId = bsn.substring(n + 1);
        ps
                .println("<project xmlns='http://maven.apache.org/POM/4.0.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd'>");
        ps.println("  <modelVersion>4.0.0</modelVersion>");
        ps.println("  <groupId>" + groupId + "</groupId>");

        n = artifactId.indexOf(';');
        if (n > 0)
            artifactId = artifactId.substring(0, n).trim();

        ps.println("  <artifactId>" + artifactId + "</artifactId>");
        ps.println("  <version>" + version + "</version>");
        if (description != null) {
            ps.println("  <description>");
            ps.print("    ");
            ps.println(description);
            ps.println("  </description>");
        }
        if (name != null) {
            ps.print("  <name>");
            ps.print(name);
            ps.println("</name>");
        }
        if (docUrl != null) {
            ps.print("  <url>");
            ps.print(docUrl);
            ps.println("</url>");
        }

        if (bundleVendor != null) {
            Matcher m = NAME_URL.matcher(bundleVendor);
            String namePart = bundleVendor;
            String urlPart = null;
            if (m.matches()) {
                namePart = m.group(1);
                urlPart = m.group(2);
            }
            ps.println("  <organization>");
            ps.print("    <name>");
            ps.print(namePart.trim());
            ps.println("</name>");
            if (urlPart != null) {
                ps.print("    <url>");
                ps.print(urlPart.trim());
                ps.println("</url>");
            }
            ps.println("  </organization>");
        }
        if (licenses != null) {
            ps.println("  <licenses>");

            Map<String, Map<String, String>> map = Processor.parseHeader(licenses, null);
            for (Iterator<Map.Entry<String, Map<String, String>>> e = map
                    .entrySet().iterator(); e.hasNext();) {

                // Bundle-License:
                // http://www.opensource.org/licenses/apache2.0.php; \
                // description="${Bundle-Copyright}"; \
                // link=LICENSE
                //
                //  <license>
                //    <name>This material is licensed under the Apache
                // Software License, Version 2.0</name>
                //    <url>http://www.apache.org/licenses/LICENSE-2.0</url>
                //    <distribution>repo</distribution>
                //    </license>

                Map.Entry<String, Map<String, String>> entry = e.next();
                ps.println("    <license>");
                Map<String, String> values = entry.getValue();
                String url = entry.getKey();

                if (values.containsKey("description"))
                    print(ps, values, "description", "name", url);
                else
                    print(ps, values, "name", "name", url);

                print(ps, values, "url", "url", url);
                print(ps, values, "distribution", "distribution", "repo");
                ps.println("    </license>");
            }
            ps.println("  </licenses>");
        }
        ps.println("</project>");
    }

    /**
     * Utility function to print a tag from a map
     * 
     * @param ps
     * @param values
     * @param string
     * @param tag
     * @param object
     */
    private void print(PrintStream ps, Map<String, String> values,
            String string, String tag, String object) {
        String value = (String) values.get(string);
        if (value == null)
            value = object;
        if (value == null)
            return;
        ps.println("    <" + tag + ">" + value.trim() + "</" + tag + ">");
    }
}
