package bndtools.jareditor.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import aQute.lib.io.IO;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;
import aQute.lib.osgi.Resource;
import aQute.lib.osgi.Verifier;
import aQute.libg.generics.Create;

public class Printer extends Processor {

    final static int MANIFEST = 2;
    final static int LIST = 4;

    final static int IMPEXP = 16;
    final static int USES = 32;
    final static int USEDBY = 64;
    final static int COMPONENT = 128;
    final static int METATYPE = 256;
    final static int VERIFY = 1;

    PrintStream out = System.out;

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void doPrint(String string, int options) throws Exception {
        File file = new File(string);
        if (!file.exists())
            error("File to print not found: " + string);
        else {
            if (options == 0)
                options = VERIFY | MANIFEST | IMPEXP | USES;
            doPrint(file, options);
        }
    }

    private void doPrint(File file, int options) throws ZipException, IOException, Exception {

        Jar jar = new Jar(file.getName(), file);
        try {
            if ((options & VERIFY) != 0) {
                Verifier verifier = new Verifier(jar);
                verifier.setPedantic(isPedantic());
                verifier.verify();
                getInfo(verifier);
            }
            if ((options & MANIFEST) != 0) {
                Manifest manifest = jar.getManifest();
                if (manifest == null)
                    warning("JAR has no manifest " + file);
                else {
                    out.println("[MANIFEST " + jar.getName() + "]");
                    SortedSet<String> sorted = new TreeSet<String>();
                    for (Object element : manifest.getMainAttributes().keySet()) {
                        sorted.add(element.toString());
                    }
                    for (String key : sorted) {
                        Object value = manifest.getMainAttributes().getValue(key);
                        format(out, "%-40s %-40s\r\n", new Object[] { key, value });
                    }
                }
                out.println();
            }
            if ((options & IMPEXP) != 0) {
                out.println("[IMPEXP]");
                Manifest m = jar.getManifest();
                if (m != null) {
                    Map<String, Map<String, String>> imports = parseHeader(m.getMainAttributes().getValue(Analyzer.IMPORT_PACKAGE));
                    Map<String, Map<String, String>> exports = parseHeader(m.getMainAttributes().getValue(Analyzer.EXPORT_PACKAGE));
                    for (String p : exports.keySet()) {
                        if (imports.containsKey(p)) {
                            Map<String, String> attrs = imports.get(p);
                            if (attrs.containsKey(Constants.VERSION_ATTRIBUTE)) {
                                exports.get(p).put("imported-as", attrs.get(VERSION_ATTRIBUTE));
                            }
                        }
                    }
                    print("Import-Package", new TreeMap<String, Map<String, String>>(imports));
                    print("Export-Package", new TreeMap<String, Map<String, String>>(exports));
                } else
                    warning("File has no manifest");
            }

            if ((options & (USES | USEDBY)) != 0) {
                out.println();
                Analyzer analyzer = new Analyzer();
                analyzer.setPedantic(isPedantic());
                analyzer.setJar(jar);
                analyzer.analyze();
                if ((options & USES) != 0) {
                    out.println("[USES]");
                    printMapOfSets(out, new TreeMap<String, Set<String>>(analyzer.getUses()));
                    out.println();
                }
                if ((options & USEDBY) != 0) {
                    out.println("[USEDBY]");
                    printMapOfSets(out, invertMapOfCollection(analyzer.getUses()));
                }
            }

            if ((options & COMPONENT) != 0) {
                printComponents(out, jar);
            }

            if ((options & METATYPE) != 0) {
                printMetatype(out, jar);
            }

            if ((options & LIST) != 0) {
                out.println("[LIST]");
                for (Map.Entry<String, Map<String, Resource>> entry : jar.getDirectories().entrySet()) {
                    String name = entry.getKey();
                    Map<String, Resource> contents = entry.getValue();
                    out.println(name);
                    if (contents != null) {
                        for (String element : contents.keySet()) {
                            int n = element.lastIndexOf('/');
                            if (n > 0)
                                element = element.substring(n + 1);
                            out.print("  ");
                            out.print(element);
                            String path = element;
                            if (name.length() != 0)
                                path = name + "/" + element;
                            Resource r = contents.get(path);
                            if (r != null) {
                                String extra = r.getExtra();
                                if (extra != null) {

                                    out.print(" extra='" + escapeUnicode(extra) + "'");
                                }
                            }
                            out.println();
                        }
                    } else {
                        out.println(name + " <no contents>");
                    }
                }
                out.println();
            }
        } finally {
            jar.close();
        }
    }

    private void print(String msg, Map<String, Map<String, String>> ports) {
        if (ports.isEmpty())
            return;
        out.println(msg);
        for (Map.Entry<String, Map<String, String>> entry : ports.entrySet()) {
            String key = entry.getKey();
            Map<String, String> clause = Create.copy(entry.getValue());
            clause.remove("uses:");
            format(out, "  %-38s %s\r\n", key.trim(), clause.isEmpty() ? "" : clause.toString());
        }
    }

    static void printMapOfSets(PrintStream out, Map<String, Set<String>> map) {
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String name = entry.getKey();
            Set<String> used = new TreeSet<String>(entry.getValue());

            for (Iterator<String> k = used.iterator(); k.hasNext();) {
                String n = k.next();
                if (n.startsWith("java.") && !n.equals("java.sql"))
                    k.remove();
            }
            String list = vertical(40, used);
            format(out, "%-40s %s", new Object[] { name, list });
        }
    }

    static void format(PrintStream out, String string, Object... objects) {
        if (objects == null || objects.length == 0)
            return;

        StringBuffer sb = new StringBuffer();
        int index = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
            case '%':
                String s = objects[index++] + "";
                int width = 0;
                int justify = -1;

                i++;

                c = string.charAt(i++);
                switch (c) {
                case '-':
                    justify = -1;
                    break;
                case '+':
                    justify = 1;
                    break;
                case '|':
                    justify = 0;
                    break;
                default:
                    --i;
                }
                c = string.charAt(i++);
                while (c >= '0' && c <= '9') {
                    width *= 10;
                    width += c - '0';
                    c = string.charAt(i++);
                }
                if (c != 's') {
                    throw new IllegalArgumentException("Invalid sprintf format:  " + string);
                }

                if (s.length() > width)
                    sb.append(s);
                else {
                    switch (justify) {
                    case -1:
                        sb.append(s);
                        for (int j = 0; j < width - s.length(); j++)
                            sb.append(" ");
                        break;

                    case 1:
                        for (int j = 0; j < width - s.length(); j++)
                            sb.append(" ");
                        sb.append(s);
                        break;

                    case 0:
                        int spaces = (width - s.length()) / 2;
                        for (int j = 0; j < spaces; j++)
                            sb.append(" ");
                        sb.append(s);
                        for (int j = 0; j < width - s.length() - spaces; j++)
                            sb.append(" ");
                        break;
                    }
                }
                break;

            default:
                sb.append(c);
            }
        }
        out.print(sb);
    }

    static String vertical(int padding, Set<String> used) {
        StringBuffer sb = new StringBuffer();
        String del = "";
        for (Iterator<String> u = used.iterator(); u.hasNext();) {
            String name = u.next();
            sb.append(del);
            sb.append(name);
            sb.append("\r\n");
            del = pad(padding);
        }
        if (sb.length() == 0)
            sb.append("\r\n");
        return sb.toString();
    }

    static String pad(int i) {
        StringBuffer sb = new StringBuffer();
        while (i-- > 0)
            sb.append(' ');
        return sb.toString();
    }

    static final String escapeUnicode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c <= '~' && c != '\\')
                sb.append(c);
            else {
                sb.append("\\u");
                sb.append(nibble(c >> 12));
                sb.append(nibble(c >> 8));
                sb.append(nibble(c >> 4));
                sb.append(nibble(c));
            }
        }
        return sb.toString();
    }

    static final char nibble(int i) {
        return "0123456789ABCDEF".charAt(i & 0xF);
    }

    static Map<String, Set<String>> invertMapOfCollection(Map<String, Set<String>> map) {
        Map<String, Set<String>> result = new TreeMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("java.") && !name.equals("java.sql"))
                continue;

            Collection<String> used = entry.getValue();
            for (String n : used) {
                if (n.startsWith("java.") && !n.equals("java.sql"))
                    continue;
                Set<String> set = result.get(n);
                if (set == null) {
                    set = new TreeSet<String>();
                    result.put(n, set);
                }
                set.add(name);
            }
        }
        return result;
    }

    void printComponents(PrintStream out, Jar jar) throws Exception {
        out.println("[COMPONENTS]");
        Manifest manifest = jar.getManifest();
        if (manifest == null) {
            out.println("No manifest");
            return;
        }

        String componentHeader = manifest.getMainAttributes().getValue(Constants.SERVICE_COMPONENT);
        Map<String, Map<String, String>> clauses = parseHeader(componentHeader);
        for (String path : clauses.keySet()) {
            out.println(path);

            Resource r = jar.getResource(path);
            if (r != null) {
                InputStreamReader ir = new InputStreamReader(r.openInputStream(), Constants.DEFAULT_CHARSET);
                OutputStreamWriter or = new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);
                try {
                    copy(ir, or);
                } finally {
                    or.flush();
                    ir.close();
                }
            } else {
                out.println("  - no resource");
                warning("No Resource found for service component: " + path);
            }
        }
        out.println();
    }

    void printMetatype(PrintStream out, Jar jar) throws Exception {
        out.println("[METATYPE]");
        Manifest manifest = jar.getManifest();
        if (manifest == null) {
            out.println("No manifest");
            return;
        }

        Map<String, Resource> map = jar.getDirectories().get("OSGI-INF/metatype");
        if (map != null) {
            for (Map.Entry<String, Resource> entry : map.entrySet()) {
                out.println(entry.getKey());
                IO.copy(entry.getValue().openInputStream(), out);
                out.println();
            }
            out.println();
        }
    }

    private static void copy(Reader rds, Writer wrt) throws IOException {
        char buffer[] = new char[1024];
        int size = rds.read(buffer);
        while (size > 0) {
            wrt.write(buffer, 0, size);
            size = rds.read(buffer);
        }
    }
}
