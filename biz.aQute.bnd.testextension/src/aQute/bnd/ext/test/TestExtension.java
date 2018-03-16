package aQute.bnd.ext.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.extension.ExtensionActivator;

public class TestExtension implements ExtensionActivator {

	@Override
	public List<?> activate(Workspace workspace, Map<String, String> attrs) {
		System.out.println("Activate test extension " + attrs);
		return Arrays.asList("hello, I am a test extension");
	}

	@Override
	public void deactivate() {
		System.out.println("Deactivate test extension");
	}

}
