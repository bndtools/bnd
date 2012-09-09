package org.bndtools.core.build.validate;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import aQute.bnd.osgi.Builder;
import bndtools.Plugin;
import bndtools.api.IValidator;

public class BsnValidator implements IValidator {

    public IStatus validate(Builder builder) {
        IStatus status = Status.OK_STATUS;

        // Get actual BSN
        String actual = builder.getBsn();

        // Get expected BSN from file name
        String expected = null;
        if (builder.getPropertiesFile() != null)
            expected = builder.getPropertiesFile().getName();
        String projectName = builder.getBase().getName();
        if (expected == null || expected.equals("bnd.bnd")) {
            expected = projectName;
        } else if (expected.endsWith(".bnd")) {
            expected = expected.substring(0, expected.length() - ".bnd".length());
            if (!expected.startsWith(builder.getBase().getName()))
                expected = projectName + "." + expected;
        }

        // Report error if not matching
        if (!actual.equals(expected))
            status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("Bundle-SymbolicName '%s' is not valid for builder: %s", actual, (builder.getPropertiesFile() == null ? builder.getBase().getName() : builder
                    .getPropertiesFile().getName())), null);

        return status;
    }

}
