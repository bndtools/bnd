package aQute.bnd.osgi;

import java.util.*;

import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.libg.reporter.*;

public interface AnalyzerMessages extends Messages {

	WARNING Export_Has_PrivateReferences_(PackageRef exported, int count, Collection<PackageRef> local);
/**/
}

