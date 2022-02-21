@Version("1.2.0")
@Export(exclude = PrivateImpl.class, mandatory = "a=b")
package test.versionpolicy.api;

import org.osgi.annotation.versioning.Version;

import aQute.bnd.annotation.Export;
