package bndtools.wizards.repo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class DummyBundle implements Bundle {

    private static final boolean DEBUG = false;

    private final long id;
    private final BundleContext context;

    DummyBundle(long id, BundleContext context) {
        this.id = id;
        this.context = context;
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
        return new Hashtable();
    }

    public Dictionary getHeaders(String arg0) {
        if (DEBUG) System.out.println("=== getHeaders(String)");
        return null;
    }

    public long getLastModified() {
        if (DEBUG) System.out.println("=== getLastModified");
        return 0;
    }

    public String getLocation() {
        if (DEBUG) System.out.println("=== getLocation");
        return null;
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
        return 0;
    }

    public String getSymbolicName() {
        if (DEBUG) System.out.println("=== getSymbolicName");
        return null;
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

}
