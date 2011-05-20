package bndtools.bindex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.impl.CapabilityImpl;
import org.osgi.framework.Constants;

import aQute.libg.header.OSGiHeader;
import bndtools.api.EE;

public class GlobalCapabilityGenerator {

    private static final String CAPABILITY_BUNDLE = "bundle";
    private static final String PROPERTY_SYMBOLICNAME = "symbolicname";

    private static final String CAPABILITY_EE = "ee";
    private static final String PROPERTY_EE = "ee";

    private static final String CAPABILITY_PACKAGE = "package";
    private static final String PROPERTY_PACKAGE = "package";
    private static final String PROPERTY_VERSION = "version";
    private static final String TYPE_VERSION = "version";

    private static final String DEFAULT_VERSION = "0.0.0";

    private final File frameworkJarFile;

    public GlobalCapabilityGenerator(File frameworkJarFile) {
        this.frameworkJarFile = frameworkJarFile;
    }

    public List<Capability> listCapabilities(EE ee) throws IOException {
        List<Capability> result = new ArrayList<Capability>();

        addEECapabilities(ee, result);

        JarFile jarFile = new JarFile(frameworkJarFile);
        try {
            addExportedPackageCapabilities(jarFile.getManifest().getMainAttributes(), result);
        } finally {
            jarFile.close();
        }

        return result;
    }

    /**
     * Generates a capability for the system bundle's symbolic name/version;
     * used by those unfortunate bundles that require a specific system bundle
     * (e.g. {@code Require-Bundle: org.eclipse.osgi}).
     *
     * @param attribs
     *            MANIFEST.MF attributes
     * @return An OBR capability
     * @throws IllegalArgumentException
     *             If the MANIFEST.MF does not have a BSN.
     */
    Capability createBundleCapability(Attributes attribs) throws IllegalArgumentException {
        String bsn = (String) attribs.get(Constants.BUNDLE_SYMBOLICNAME);
        if (bsn == null)
            throw new IllegalArgumentException("System bundle file does not have a Bundle-SymbolicName header.");
        int semicolonIndex = bsn.indexOf(';');
        if (semicolonIndex >= 0)
            bsn = bsn.substring(0, semicolonIndex);
        String version = (String) attribs.get(Constants.BUNDLE_VERSION);
        if (version == null)
            version = DEFAULT_VERSION;

        CapabilityImpl capability = new CapabilityImpl(CAPABILITY_BUNDLE);
        capability.addProperty(PROPERTY_SYMBOLICNAME, bsn);
        capability.addProperty(PROPERTY_VERSION, TYPE_VERSION, version);
        return capability;
    }

    /**
     * Adds capabilities for the the system bundle exports (i.e. mostly
     * {@code org.osgi.* }).
     *
     * @param attribs
     *            MANIFEST.MF main attributes
     * @param capabilities
     *            A list of capabilities to contain the results.
     */
    void addExportedPackageCapabilities(Attributes attribs, List<Capability> capabilities) {
        String exportPkgsStr = attribs.getValue(Constants.EXPORT_PACKAGE);
        if (exportPkgsStr == null || exportPkgsStr.length() == 0)
            return;

        Map<String, Map<String, String>> exports = OSGiHeader.parseHeader(exportPkgsStr);
        for (Entry<String, Map<String, String>> entry : exports.entrySet()) {
            capabilities.add(createPackageCapability(entry.getKey(), entry.getValue().get(Constants.VERSION_ATTRIBUTE)));
        }
    }

    void addEECapabilities(EE ee, List<Capability> capabilities) throws IOException {
        // EE Capabilities
        for (EE compatible : ee.getCompatible()) {
            capabilities.add(createEECapability(compatible));
        }
        capabilities.add(createEECapability(ee));

        // EE Package Capabilities
        Properties pkgProps = new Properties();
        URL pkgsResource = GlobalCapabilityGenerator.class.getResource(ee.name() + ".properties");
        InputStream stream = null;
        try {
            stream = pkgsResource.openStream();
            pkgProps.load(stream);
        } finally {
            if (stream != null)
                stream.close();
        }
        String pkgsStr = pkgProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
        Map<String, Map<String, String>> header = OSGiHeader.parseHeader(pkgsStr);
        for (Entry<String, Map<String, String>> entry : header.entrySet()) {
            capabilities.add(createPackageCapability(entry.getKey(), entry.getValue().get(Constants.VERSION_ATTRIBUTE)));
        }
    }

    Capability createPackageCapability(String pkgName, String version) {
        CapabilityImpl capability = new CapabilityImpl(CAPABILITY_PACKAGE);
        capability.addProperty(PROPERTY_PACKAGE, pkgName);
        capability.addProperty(PROPERTY_VERSION, TYPE_VERSION, version != null ? version : DEFAULT_VERSION);
        return capability;
    }

    Capability createEECapability(EE ee) {
        CapabilityImpl capability = new CapabilityImpl(CAPABILITY_EE);
        capability.addProperty(PROPERTY_EE, ee.getEEName());
        return capability;
    }

}