package aQute.lib.jardiff;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.lib.osgi.*;

public class Diff {

    /**
     * Compare two JAR files with each other.
     * 
     * @param a
     * @param b
     * @param strict
     * @return
     * @throws IOException
     */
    public Map<String, Object> diff(Jar a, Jar b, boolean strict)
            throws Exception {
        Map<String, Object> different = new TreeMap<String, Object>();
        compareManifest(different, a.getManifest(), b.getManifest(), strict);
        diff(different, a.getResources().keySet(), b.getResources().keySet());

        Set<String> shared = new HashSet<String>(a.getResources().keySet());
        shared.retainAll(b.getResources().keySet());

        for (String path : a.getResources().keySet()) {
            Resource ra = a.getResource(path);
            Resource rb = a.getResource(path);
            if (rb != null) {
                if (ra.getClass() != rb.getClass()) {
                    different.put(path, "Different types: "
                            + a.getClass().getName() + " : "
                            + b.getClass().getName());
                } else {
                    if (path.endsWith(".jar")) {
                        Jar aa = new Jar(path, ra.openInputStream());
                        Jar bb = new Jar(path, rb.openInputStream());
                        try {
                            Map<String, Object> result = diff(aa, bb, strict);
                            if (!result.isEmpty())
                                different.put(path, result);
                        } finally {
                            aa.close();
                            bb.close();
                        }
                    } else {
                        String cmp = diff(ra.openInputStream(), rb
                                .openInputStream());
                        if (cmp != null)
                            different.put(path, cmp);
                    }
                }
            }
        }
        return different;
    }

    String diff(InputStream a, InputStream b) throws IOException {
        int n = 0;
        int binary = 0;
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ac = a.read();
            int bc = b.read();
            if (ac < 0) {
                if (bc < 0)
                    return null;

                return "a is smaller";
            } else if (bc < 0) {
                return "b is smaller";
            }

            if (ac != bc) {
                String s = "Difference at pos: " + n;
                if (binary == 0 && sb.length() > 5) {
                    s = s + "Context: " + sb.substring(sb.length() - 5);
                }
            }

            if (ac >= ' ' && ac <= '~')
                sb.append((char) ac);
            else
                binary++;

            n++;
        }
    }

    void diff(Map<String, Object> different, Set<?> a, Set<?> b) {
        Set<Object> onlyInA = new HashSet<Object>(a);
        onlyInA.removeAll(b);
        Set<Object> onlyInB = new HashSet<Object>(b);
        onlyInB.removeAll(a);

        for (Object element : onlyInA) {
            different.put(element.toString(), "a");
        }
        for (Object element : onlyInB) {
            different.put(element.toString(), "b");
        }
    }

    public void compareManifest(Map<String, Object> different, Manifest a,
            Manifest b, boolean strict) {
        if (a == null || b == null) {
            different.put("Manifest null", (a == null ? "a=null" : "a exists")
                    + " " + (b == null ? "b=null" : "b exists"));
            return;
        }

        Attributes attrs = a.getMainAttributes();
        Attributes bttrs = b.getMainAttributes();
        diff(different, attrs.keySet(), bttrs.keySet());
        for (Object element : attrs.keySet()) {
            Attributes.Name name = (Attributes.Name) element;
            String av = attrs.getValue(name);
            String bv = bttrs.getValue(name);
            if (bv != null) {
                if (!av.equals(bv))
                    different.put(name.toString(), "M:" + name + ":" + av
                            + "!=" + bv);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void print(PrintStream pout, Map<String, Object> map, int indent) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            for (int j = 0; j < indent; j++) {
                pout.print(" ");
            }
            String key = entry.getKey();
            pout.print(key);
            for (int j = 0; j < 70 - indent - key.length(); j++) {
                pout.print(" ");
            }
            if (entry.getValue() instanceof Map) {
                pout.println();
                print(pout, (Map<String, Object>) entry.getValue(), indent + 1);
            } else
                pout.println(entry.getValue());
        }
    }

    public void close() {

    }
}
