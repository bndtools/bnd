package test;

import java.io.*;

import junit.framework.*;
import aQute.jpm.lib.*;
import aQute.jpm.platform.*;
import aQute.libg.reporter.*;
import aQute.service.reporter.*;

public class JPMTest extends TestCase {
	File	cwd	= new File(System.getProperty("user.dir")).getAbsoluteFile();

	class PLF extends Unix {

		@Override
		public void shell(String initial) throws Exception {}

		@Override
		public String getName() {
			return "Test Platform";
		}

		@Override
		public void uninstall() {}

		@Override
		public File getGlobal() {
			return new File(cwd, "global").getAbsoluteFile();
		}

		@Override
		public File getLocal() {
			return new File(cwd, "local").getAbsoluteFile();
		}

		@Override
		public String createCommand(CommandData data) throws Exception {
			return null;
		}

		@Override
		public String createService(ServiceData data) throws Exception {
			return null;
		}
	}

	public void testSimple() {
		Reporter r = new ReporterAdapter();
		JustAnotherPackageManager jpm = new JustAnotherPackageManager(r);
		Platform plf = new PLF();
		jpm.setPlatform(plf);

	}
}
