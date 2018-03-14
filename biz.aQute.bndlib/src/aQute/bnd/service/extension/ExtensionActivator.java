package aQute.bnd.service.extension;

import java.util.List;
import java.util.Map;

import aQute.bnd.build.Workspace;

public interface ExtensionActivator {
	List<?> activate(Workspace workspace, Map<String, String> attrs);

	void deactivate();
}
