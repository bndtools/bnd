package aQute.bnd.service.extension;

import java.util.*;

import aQute.bnd.build.*;

public interface ExtensionActivator {
	List< ? > activate(Workspace workspace, Map<String,String> attrs);
	void deactivate();
}
