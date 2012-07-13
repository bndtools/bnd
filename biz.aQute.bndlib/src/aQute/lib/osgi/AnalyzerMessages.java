package aQute.lib.osgi;

import java.util.*;

import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.libg.reporter.*;

public interface AnalyzerMessages extends Messages {

	WARNING Export_HasPrivateReferences_(PackageRef exported, Collection<PackageRef> local);
/**/
}

