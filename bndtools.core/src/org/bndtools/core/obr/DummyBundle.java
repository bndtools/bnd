package org.bndtools.core.obr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class DummyBundle implements Bundle {

    private static final boolean DEBUG = false;

    private final long id;
    private final BundleContext context;
    private final File jar;

    private Properties props;


    @SuppressWarnings("deprecation")
    DummyBundle(long id, BundleContext context, File jar) throws IOException {
        this.id = id;
        this.context = context;
        this.jar = jar;

        JarInputStream stream = new JarInputStream(new FileInputStream(jar));
        Manifest manifest = stream.getManifest();

        props = new Properties() {
            @Override
            public Object get(Object key) {
                Object value = super.get(key);
                if (DEBUG) System.out.println("=== getHeaders --> " + key + " = " + value);
                return value;
            }
        };
        Attributes attribs = manifest.getMainAttributes();
        for (Object key : attribs.keySet()) {
            String name = key.toString();
            props.put(name, attribs.getValue(name));
        }
    }

    @SuppressWarnings("unchecked")
    private boolean copy(Attributes from, @SuppressWarnings("rawtypes") Dictionary to, String name) {
        String value = from.getValue(name);
        if (value != null) {
            to.put(name, value);
            return true;
        }
        return false;
    }

    public Enumeration findEntries(String arg0, String arg1, boolean arg2) {
        if (DEBUG) System.out.println("=== findEntries");
        return null;
    }

    public BundleContext getBundleContext() {
        if (DEBUG) System.out.println("=== getBundleContext");
        return context;
    }

    public long getBundleId() {
        if (DEBUG) System.out.println("=== getBundleId");
        return id;
    }

    public URL getEntry(String arg0) {
        if (DEBUG) System.out.println("=== getEntry");
        return null;
    }

    public Enumeration getEntryPaths(String arg0) {
        if (DEBUG) System.out.println("=== getEntryPaths");
        return null;
    }

    public Dictionary getHeaders() {
        if (DEBUG) System.out.println("=== getHeaders");
        return props;
    }

    public Dictionary getHeaders(String arg0) {
        if (DEBUG) System.out.println("=== getHeaders(String)");
        return props;
    }

    public long getLastModified() {
        if (DEBUG) System.out.println("=== getLastModified");
        return 0;
    }

    public String getLocation() {
        if (DEBUG) System.out.println("=== getLocation");
        return Constants.SYSTEM_BUNDLE_LOCATION;
    }

    public ServiceReference[] getRegisteredServices() {
        if (DEBUG) System.out.println("=== getRegisteredServices");
        return null;
    }

    public URL getResource(String arg0) {
        if (DEBUG) System.out.println("=== getResource");
        return null;
    }

    public Enumeration getResources(String arg0) throws IOException {
        if (DEBUG) System.out.println("=== getResources");
        return null;
    }

    public ServiceReference[] getServicesInUse() {
        if (DEBUG) System.out.println("=== getServicesInUse");
        return null;
    }

    public int getState() {
        if (DEBUG) System.out.println("=== getState");
        return ACTIVE;
    }

    public String getSymbolicName() {
        if (DEBUG) System.out.println("=== getSymbolicName");
        return props.getProperty(Constants.BUNDLE_SYMBOLICNAME);
    }

    public boolean hasPermission(Object arg0) {
        if (DEBUG) System.out.println("=== hasPermission");
        return true;
    }

    public Class loadClass(String arg0) throws ClassNotFoundException {
        if (DEBUG) System.out.println("=== loadClass(String)");
        return null;
    }

    public void start() throws BundleException {
        if (DEBUG) System.out.println("=== start");
    }

    public void start(int arg0) throws BundleException {
        if (DEBUG) System.out.println("=== start(int)");
    }

    public void stop() throws BundleException {
        if (DEBUG) System.out.println("=== stop");
    }

    public void stop(int arg0) throws BundleException {
        if (DEBUG) System.out.println("=== stop(int)");
    }

    public void uninstall() throws BundleException {
        if (DEBUG) System.out.println("=== uninstall");
    }

    public void update() throws BundleException {
        if (DEBUG) System.out.println("=== update");
    }

    public void update(InputStream arg0) throws BundleException {
        if (DEBUG) System.out.println("=== update(InputStream)");
    }

    public Map getSignerCertificates(int arg0) {
        return null;
    }

    public Version getVersion() {
        return null;
    }

}
