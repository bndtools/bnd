package aQute.bnd.osgi;

import java.util.*;

import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.service.reporter.*;

public interface AnalyzerMessages extends Messages {

	WARNING Export_Has_PrivateReferences_(PackageRef exported, int count, Collection<PackageRef> local);
/**/
}

