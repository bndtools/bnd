package aQute.junit.runtime;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.service.permissionadmin.*;

import aQute.junit.osgi.*;
import aQute.junit.runtime.minifw.*;

public class GenericFramework {
    private String                 framework;
    private final List            /* <File> */jars             = new ArrayList/* <File> */();
    private final List            /* <File> */bundles          = new ArrayList/* <File> */();
    private final List            /* <String> */systemPackages = new ArrayList/* <String> */();
    private Framework              systemBundle;
    private File                   storage;
    private boolean                keep;
    private final Properties       properties;
    private boolean                security;
    private SimplePermissionPolicy policy;

    public GenericFramework(Properties properties) {
        this.properties = properties;
        systemPackages.add("org.osgi.framework");
        systemPackages.add("org.osgi.framework.launch");
    }

    public boolean activate() throws Exception {
        boolean error = false;
        Policy.setPolicy(new AllPolicy());

        systemBundle = createFramework();
        if (systemBundle == null)
            return false;

        try {
            PermissionInfo         allPermissions[]          = new PermissionInfo[] { new PermissionInfo(
                    AllPermission.class
                            .getName(),
                    null, null) };
            policy = new SimplePermissionPolicy(systemBundle.getBundleContext());

            // All bundles installed from the script are getting AllPermission
            // for now.
            policy.setDefaultPermissions(allPermissions);
            security = true;
        } catch (Throwable t) {
            // This can throw a linkage error when the framework
            // does not carry the PermissionAdmin class
            security = false;
        }

        systemBundle.start();
        // Initialize this framework so it becomes STARTING
        BundleContext systemContext = getFrameworkContext();

        // Install the set of bundles
        List/* <Bundle> */installed = new ArrayList/* <Bundle> */();

        for (Iterator/* <File> */i = bundles.iterator(); i.hasNext();) {
            File path = ((File) i.next()).getAbsoluteFile();

            InputStream in = new FileInputStream(path);
            try {
                Bundle bundle = systemContext
                        .installBundle(path.toString(), in);
                installed.add(bundle);
            } catch (BundleException e) {
                System.out.println("Install: " + path + " ");
                report(e, System.out);
                error = true;
            } finally {
                in.close();
            }
        }

        // From now on, the bundles are on their own. They have
        // by default AllPermission, but if they install bundles
        // they will not automatically get AllPermission anymore
        
        if (security)
            policy.setDefaultPermissions(null);

        // Now start all the installed bundles in the same order
        // (unless they're a fragment)
        
        for (Iterator/* <Bundle> */i = installed.iterator(); i.hasNext();) {
            Bundle b = (Bundle) i.next();
            try {
                if (!isFragment(b))
                    b.start();
            } catch (BundleException e) {
                System.out.println("Start: " + b.getBundleId() + " ");
                report(e, System.out);
                error = true;
            }
        }
        return !error;
    }

    private boolean isFragment(Bundle b) {
        return b.getHeaders().get("Fragment-Host") != null;
    }

    public void deactivate() throws Exception {
        if (systemBundle != null) {
            getFrameworkBundle().stop();
            waitForStop(0);
        }
    }

    public void addSystemPackage(String packageName) {
        systemPackages.add(packageName);
    }

    private Framework createFramework() throws Exception {
        Properties p = new Properties();
        p.putAll(properties);

        if (storage != null) {
            if (!keep)
                delete(storage);

            storage.mkdirs();
            if (!storage.isDirectory())
                throw new IllegalArgumentException();

            p.setProperty(Constants.FRAMEWORK_STORAGE, storage
                    .getAbsolutePath());
        }

        if (!systemPackages.isEmpty())
            p.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
                    toPackages(systemPackages));

        URL urls[] = new URL[jars.size()];
        for (int i = 0; i < jars.size(); i++) {
            urls[i] = ((File) jars.get(i)).toURL();
        }

        URLClassLoader loader = new URLClassLoader(urls, getClass()
                .getClassLoader());

        try {
            Framework systemBundle;

            // 1) framework = Framework class name as originally defined
            // 2) framework = Framework Factory class name, new!
            // 3) framework = null, lookup in META-INF/services

            if (p.containsKey("noframework")) {
                systemBundle = new MiniFramework(p);
            } else if (framework != null) {
                Class/* <?> */clazz = loader.loadClass(framework);

                if (FrameworkFactory.class.isAssignableFrom(clazz)) {
                    // 2) Specified factory name
                    FrameworkFactory f = (FrameworkFactory) clazz.newInstance();
                    systemBundle = f.newFramework(p);
                } else if (Framework.class.isAssignableFrom(clazz)) {
                    // 1) Specified framework name
                    Constructor/* <?> */ctor = clazz
                            .getConstructor(new Class[] { Map.class });

                    systemBundle = (Framework) ctor
                            .newInstance(new Object[] { p });
                } else
                    throw new IllegalArgumentException(
                            "Specified framework class is not an instance class nor a framework factory");

            } else {
                // 3) Lookup in META-INF/services
                List implementations = getMetaInfServices(loader,
                        FrameworkFactory.class.getName());
                if (implementations.size() != 1)
                    System.out
                            .println("Found more than one framework implementatios: "
                                    + implementations);

                String implementation = (String) implementations.get(0);

                Class/* <?> */clazz = loader.loadClass(implementation);
                FrameworkFactory factory = (FrameworkFactory) clazz
                        .newInstance();
                systemBundle = factory.newFramework(p);
            }
            systemBundle.init();
            return systemBundle;
        } catch (ClassNotFoundException cnfe) {
            System.out
                    .println("Can not load the framework class: " + framework);
            return null;
        } catch (NoSuchMethodException nsme) {
            System.out
                    .println("Can not find RFC 132 constructor <init>(Map) in "
                            + framework);
            return null;
        } catch (InvocationTargetException e) {
            System.out.println("Error in constructing framework");
            e.getCause().printStackTrace();
            throw e;
        }
    }

    /**
     * Try to get the stupid service interface ...
     * 
     * @param loader
     * @param string
     * @return
     * @throws IOException
     */
    private List getMetaInfServices(ClassLoader loader, String factory)
            throws IOException {
        if (loader == null)
            loader = getClass().getClassLoader();

        Enumeration e = loader.getResources("META-INF/services/" + factory);
        List factories = new ArrayList();

        while (e.hasMoreElements()) {
            URL url = (URL) e.nextElement();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(url
                    .openStream()));
            String line;
            while ((line = rdr.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#") && line.length() > 0) {
                    factories.add(line);
                }
            }
        }
        return factories;
    }

    private String toPackages(List/* <String> */packs) {
        String del = "";
        StringBuffer sb = new StringBuffer();
        for (Iterator i = packs.iterator(); i.hasNext();) {
            String s = (String) i.next();
            sb.append(del);
            sb.append(s);
            del = ", ";
        }
        return sb.toString();
    }

    public void addBundle(File resource) {
        bundles.add(resource);
    }

    public void addJar(File resource) {
        jars.add(resource);
    }

    public BundleContext getFrameworkContext() {
        return systemBundle.getBundleContext();
    }

    public Framework getFrameworkBundle() {
        return systemBundle;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getFramework() {
        return framework;
    }

    public BundleContext getBundleContext() {
        return systemBundle.getBundleContext();
    }

    public void waitForStop(long time) throws Exception {
        getFrameworkBundle().waitForStop(time);
    }

    public Bundle getBundle(String target) {
        Bundle bundles[] = getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].getLocation().equals(target)) {
                return bundles[i];
            }
        }
        return null;
    }

    public void setKeep() {
        this.keep = true;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }

    void delete(File f) {
        String path = f.getAbsolutePath();
        char first = path.charAt(0);
        if (path.equals("/") || (first>='A' && first <='Z' && path.substring(1).equals(":\\")))
            throw new IllegalArgumentException(
                    "You can not make the root the storage area because it will be deleted");
        if (f.isDirectory()) {
            File fs[] = f.listFiles();
            for (int i = 0; i < fs.length; i++)
                delete(fs[i]);
        }
        f.delete();
    }

    public void report(PrintStream out) {
        try {
            System.out
                    .println("------------------------------- REPORT --------------------------");
            out.println();
            out.println("Framework             " + framework);
            out.println("Framework             "
                    + (systemBundle == null ? "<>" : systemBundle.getClass()
                            .toString()));
            out.println("Storage               " + storage);
            out.println("Keep                  " + keep);
            out.println("Security              " + security);
            list("Jars                  ", jars);
            list("System Packages       ", systemPackages);
            list("Classpath             ", Arrays.asList(Activator.split(System.getProperty(
                    "java.class.path"),File.pathSeparator)));
            out.println("Properties");
            for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                out.print(fill(key, 40));
                out.println(value);
            }
            if (systemBundle != null) {
                BundleContext context = systemBundle.getBundleContext();
                if (context != null) {
                    Bundle bundles[] = context.getBundles();
                    System.out.println();
                    System.out.println("Id    State Modified      Location");

                    for (int i = 0; i < bundles.length; i++) {
                        File f = new File(bundles[i].getLocation());
                        out.print(fill(Long.toString(bundles[i].getBundleId()),
                                6));
                        out.print(fill(toState(bundles[i].getState()), 6));
                        if (f.exists())
                            out.print(fill(toDate(f.lastModified()), 14));
                        else
                            out.print(fill("<>", 14));
                        out.println(bundles[i].getLocation());
                    }
                }
            }
        } catch (Throwable t) {
            out.println("Sorry, can't print framework: " + t);
        }
    }

    String toDate(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return fill(c.get(Calendar.YEAR), 4) + fill(c.get(Calendar.MONTH), 2)
                + fill(c.get(Calendar.DAY_OF_MONTH), 2)
                + fill(c.get(Calendar.HOUR_OF_DAY), 2)
                + fill(c.get(Calendar.MINUTE), 2);
    }

    private String fill(int n, int width) {
        return fill(Integer.toString(n), width, '0', -1);
    }

    private String fill(String s, int width) {
        return fill(s, width, ' ', -1);
    }

    private String fill(String s, int width, char filler, int dir) {
        StringBuffer sb = new StringBuffer();
        if (s.length() > width)
            s = s.substring(0, width - 2) + "..";

        width -= s.length();
        int before = (dir == 0) ? width / 2 : (dir < 0) ? 0 : width;
        int after = width - before;

        while (before-- > 0)
            sb.append(filler);

        sb.append(s);

        while (after-- > 0)
            sb.append(filler);

        return sb.toString();
    }

    private String toState(int state) {
        switch (state) {
        case Bundle.INSTALLED:
            return "INSTL";
        case Bundle.RESOLVED:
            return "RSLVD";
        case Bundle.STARTING:
            return "STRTD";
        case Bundle.STOPPING:
            return "STPPD";
        case Bundle.ACTIVE:
            return "ACTIV";
        case Bundle.UNINSTALLED:
            return "UNNST";
        }
        return "? " + state;
    }

    private void list(String del, List l) {
        for (Iterator i = l.iterator(); i.hasNext();) {
            String s = i.next().toString();
            System.out.println(del + s);
            del = "                                                                       "
                    .substring(0, del.length());
        }
    }

    public static void report(BundleException e, PrintStream out) {
        switch (e.getType()) {
        case BundleException.ACTIVATOR_ERROR:
            System.out.println("Caused by in activator: ");
            e.getCause().printStackTrace();
            break;

        default:
        case BundleException.DUPLICATE_BUNDLE_ERROR:
        case BundleException.INVALID_OPERATION:
        case BundleException.MANIFEST_ERROR:
        case BundleException.NATIVECODE_ERROR:
        case BundleException.STATECHANGE_ERROR:
        case BundleException.UNSUPPORTED_OPERATION:
        case BundleException.UNSPECIFIED:
        case BundleException.RESOLVE_ERROR:
            System.out.println(e.getMessage());
            break;
        }
    }

    static class AllPolicy extends Policy {
        static PermissionCollection all = new AllPermissionCollection();

        public PermissionCollection getPermissions(CodeSource codesource) {
            return all;
        }

        public void refresh() {
        }
    }

    static class AllPermissionCollection extends PermissionCollection {
        private static final long serialVersionUID = 1L;
        private static Vector     list             = new Vector();

        {
            setReadOnly();
        }

        public void add(Permission permission) {
        }

        public Enumeration elements() {
            return list.elements();
        }

        public boolean implies(Permission permission) {
            return true;
        }
    }
}
