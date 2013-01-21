package test;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;
import aQute.jpm.lib.*;
import aQute.jpm.platform.*;
import aQute.libg.reporter.*;
import aQute.service.library.Library.Revision;
import aQute.service.reporter.*;

public class JPMTest extends TestCase {
	static File	cwd	= new File(System.getProperty("user.dir")).getAbsoluteFile();

	static class PLF extends Unix {

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
		public String createCommand(CommandData data, String ...strings ) throws Exception {
			return null;
		}

		@Override
		public String createService(ServiceData data) throws Exception {
			return null;
		}

		@Override
		public void installDaemon(boolean user) throws Exception {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void uninstallDaemon(boolean user) throws Exception {
			// TODO Auto-generated method stub
			
		}
	}

	public void testCandidates() throws Exception {
		Reporter r = new ReporterAdapter();
		JustAnotherPackageManager jpm = new JustAnotherPackageManager(r);
		jpm.setPlatform( new PLF());
		jpm.setLibrary(new URI("http://localhost:8080/rest"));

		ArtifactData a = jpm.getCandidate("aQute.libg", false);
		assertNotNull(a);
		
		a.sync();
		assertNull(a.error);
		
		
		List<Revision> candidates = jpm.getCandidates("hello");
		assertNotNull(candidates);
		
		candidates = jpm.getCandidates("aQute.libg");
		assertNotNull(candidates);
	}
	
	
	public static void testSimple() throws IOException {
		Reporter r = new ReporterAdapter();
		JustAnotherPackageManager jpm = new JustAnotherPackageManager(r);
		Platform plf = new PLF();
		jpm.setPlatform(plf);

	}
	
	public static void testDownload() throws Exception {
		ReporterAdapter reporter = new ReporterAdapter();
		JustAnotherPackageManager jpm = new JustAnotherPackageManager(reporter);
		jpm.setLibrary(new URI("http://localhost:8080/rest"));
		ArtifactData artifact = jpm.getCandidate("aQute.libg", true);
		assertNotNull(artifact);
	}
}
