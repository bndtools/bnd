package bndtools.wizards.obr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class DummyBundleContext implements BundleContext {

    private static final boolean DEBUG = true;

    private final File systemBundleFile;
    private final DummyBundle systemBundle;;

    public DummyBundleContext(File systemBundleFile) throws IOException {
        this.systemBundleFile = systemBundleFile;
        this.systemBundle = new DummyBundle(0, this, systemBundleFile);
    }

    public void addBundleListener(BundleListener arg0) {
        if (DEBUG) System.out.println("--- addBundleListener(BundleListener)");
    }

    public void addFrameworkListener(FrameworkListener arg0) {
        if (DEBUG) System.out.println("--- addFrameworkListener(FrameworkListener)");
    }

    public void addServiceListener(ServiceListener arg0) {
        if (DEBUG) System.out.println("--- addServiceListener(ServiceListener)");
    }

    public void addServiceListener(ServiceListener arg0, String arg1) throws InvalidSyntaxException {
        if (DEBUG) System.out.println("--- addServiceListener(ServiceListener, String)");
    }

    public Filter createFilter(String arg0) throws InvalidSyntaxException {
        if (DEBUG) System.out.println("--- createFilter(String)");
        return FrameworkUtil.createFilter(arg0);
    }

    public ServiceReference[] getAllServiceReferences(String arg0, String arg1) throws InvalidSyntaxException {
        if (DEBUG) System.out.println("--- getAllServiceReferences(String, String)");
        return null;
    }

    public Bundle getBundle() {
        if (DEBUG) System.out.println("--- getBundle()");
        return systemBundle;
    }

    public Bundle getBundle(long id) {
        if (DEBUG) System.out.println("--- getBundle(" + id + ")");
        if (id == 0)
            return systemBundle;
        return null;
    }

    public Bundle[] getBundles() {
        if (DEBUG) System.out.println("--- getBundles()");
        return new Bundle[] { systemBundle };
    }

    public File getDataFile(String arg0) {
        if (DEBUG) System.out.println("--- getDataFile()");
        return null;
    }

    public String getProperty(String arg0) {
        if (DEBUG) System.out.println("--- getProperty(String)");
        return null;
    }

    public Object getService(ServiceReference arg0) {
        if (DEBUG) System.out.println("--- getService(ServiceReference)");
        return null;
    }

    public ServiceReference getServiceReference(String arg0) {
        if (DEBUG) System.out.println("--- getServiceReference(String)");
        return null;
    }

    public ServiceReference[] getServiceReferences(String arg0, String arg1) throws InvalidSyntaxException {
        if (DEBUG) System.out.println("--- getServiceReferences(String, String)");
        return null;
    }

    public Bundle installBundle(String arg0) throws BundleException {
        if (DEBUG) System.out.println("--- installBundle(String)");
        return null;
    }

    public Bundle installBundle(String arg0, InputStream arg1) throws BundleException {
        if (DEBUG) System.out.println("--- installBundle(String, InputStream)");
        return null;
    }

    public ServiceRegistration registerService(String[] arg0, Object arg1, Dictionary arg2) {
        if (DEBUG) System.out.println("--- registerService(String[], Object, Dictionary)");
        return null;
    }

    public ServiceRegistration registerService(String arg0, Object arg1, Dictionary arg2) {
        if (DEBUG) System.out.println("--- registerService(String, Object, Dictionary)");
        return null;
    }

    public void removeBundleListener(BundleListener arg0) {
        if (DEBUG) System.out.println("--- removeBundleListener()");
    }

    public void removeFrameworkListener(FrameworkListener arg0) {
        if (DEBUG) System.out.println("--- removeFrameworkListener()");
    }

    public void removeServiceListener(ServiceListener arg0) {
        if (DEBUG) System.out.println("--- removeServiceListener()");
    }

    public boolean ungetService(ServiceReference arg0) {
        if (DEBUG) System.out.println("--- ungetService(ServiceReference)");
        return false;
    }

}
