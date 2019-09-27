package bndtools.test.plugin;

import java.util.Map;

import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

public class TestPlugin implements Plugin {

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		// System.out.println("Yes, worked!");
	}

	@Override
	public void setReporter(Reporter processor) {
		// TODO Auto-generated method stub

	}

}
