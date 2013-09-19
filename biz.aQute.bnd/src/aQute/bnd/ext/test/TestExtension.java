package aQute.bnd.ext.test;

import java.util.*;

import aQute.bnd.build.*;
import aQute.bnd.service.extension.*;

public class TestExtension implements ExtensionActivator {

	public List<?> activate(Workspace workspace, Map<String,String> attrs) {
		System.out.println("Activate test extension " + attrs);
		return Arrays.asList("hello, I am a test extension");
	}

	public void deactivate() {
		System.out.println("Deactivate test extension");
	}

}
