package bndtools.bindex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.impl.CapabilityImpl;
import org.osgi.framework.Constants;

import aQute.libg.header.OSGiHeader;

public class FelixGlobalCapabilityGenerator {

    private final File felix;
    private Properties properties;

    public FelixGlobalCapabilityGenerator(File felix) {
        this.felix = felix;
    }

    public void initialise(String javaSpecVersion) throws IOException {
        if (properties == null)
            properties = loadProperties(felix);
        properties.put("java.specification.version", javaSpecVersion);
    }

    public List<Capability> getCapabilities() {
        List<Capability> result = new ArrayList<Capability>();

        addEECapabilities(result);
        addPackageCapabilities(result);

        return result;
    }


    Properties loadProperties(File file) throws ZipException, IOException {
        Properties properties = new Properties();

        ZipFile zipFile = new ZipFile(file);
        try {
            ZipEntry propsEntry = zipFile.getEntry("default.properties");
            if (propsEntry == null) {
                throw new IOException("Felix default.properties not found in JAR");
            }
            properties.load(zipFile.getInputStream(propsEntry));
        } finally {
            zipFile.close();
        }
        return properties;
    }

    void addEECapabilities(Collection<? super Capability> capabilities) {
        String key = "org.osgi.framework.executionenvironment";

        String ee = properties.getProperty(key);
        ee = substVars(ee, key, null, properties);

        StringTokenizer tokenizer = new StringTokenizer(ee, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();

            CapabilityImpl capability = new CapabilityImpl("ee");
            capability.addProperty("ee", token);
            capabilities.add(capability);
        }
    }

    void addPackageCapabilities(Collection<? super Capability> capabilities) {
        String key = "org.osgi.framework.system.packages";

        String syspkgs = properties.getProperty(key);
        syspkgs = substVars(syspkgs, key, null, properties);

        Map<String, Map<String, String>> exports = OSGiHeader.parseHeader(syspkgs);
        for (Entry<String, Map<String,String>> entry : exports.entrySet()) {
            CapabilityImpl capability = new CapabilityImpl("package");
            capability.addProperty("package", entry.getKey());

            String version = entry.getValue().get(Constants.VERSION_ATTRIBUTE);
            if (version == null)
                version = "0.0.0";
            capability.addProperty("version", "version", version);

            capabilities.add(capability);
        }
    }

    private static final String DELIM_START = "${";
    private static final String DELIM_STOP  = "}";


    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from inner most to outer most. Configuration
     * properties override system properties.
     * </p>
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
    **/
    public static String substVars(String val, String currentKey,
        Map cycleMap, Properties configProps)
        throws IllegalArgumentException
    {
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
        if (cycleMap == null)
        {
            cycleMap = new HashMap();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = -1;
        int startDelim = -1;

        do
        {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            // If there is no stopping delimiter, then just return
            // the value since there is no variable declared.
            if (stopDelim < 0)
            {
                return val;
            }
            // Try to find the matching start delimiter by
            // looping until we find a start delimiter that is
            // greater than the stop delimiter we have found.
            startDelim = val.indexOf(DELIM_START);
            // If there is no starting delimiter, then just return
            // the value since there is no variable declared.
            if (startDelim < 0)
            {
                return val;
            }
            while (stopDelim >= 0)
            {
                int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
                if ((idx < 0) || (idx > stopDelim))
                {
                    break;
                }
                else if (idx < stopDelim)
                {
                    startDelim = idx;
                }
            }
        }
        while ((startDelim > stopDelim) && (stopDelim >= 0));

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable =
            val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null)
        {
            throw new IllegalArgumentException(
                "recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
            ? configProps.getProperty(variable, null)
            : null;
        if (substValue == null)
        {
            // Ignore unknown property values.
            substValue = System.getProperty(variable, "");
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim)
            + substValue
            + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }
}
