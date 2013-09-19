package aQute.bnd.service.extension;

import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.header.*;

public interface ExtensionActivator {
	List< ? > activate(Workspace workspace, Map<String,String> attrs);
	void deactivate();
}
