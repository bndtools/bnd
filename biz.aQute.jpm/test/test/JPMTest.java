package test;

import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.jpm.lib.*;
import aQute.jpm.main.*;
import aQute.jpm.platform.*;
import aQute.lib.io.*;
import aQute.lib.settings.*;

public class JPMTest extends TestCase {
	
	static Settings settings;
	
	@Override
	public void setUp() {
		settings = new Settings("/tmp/settings");
	}
	
	@Override
	public void tearDown() {
		settings.clear();
	}
	
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
		public String createCommand(CommandData data, Map<String,String> map, String ...strings ) throws Exception {
			return null;
		}

		@Override
		public String createService(ServiceData data, Map<String,String> map, String ...strings ) throws Exception {
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

	/*public void testCandidates() throws Exception {
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
	}*/
	
	public void test_userMode_cache() throws Exception {
		JustAnotherPackageManager mock = mock(JustAnotherPackageManager.class);
		String[] args = {"-s", "/tmp/settings", "-c", "/tmp/cache"};
		Main main = new Main(mock);
		main.run(args);
		
		verify(mock).setHomeDir(new File("/tmp/cache"));
	}
	
	public void test_userMode_local_noConfig() throws Exception {
		JustAnotherPackageManager mock = mock(JustAnotherPackageManager.class);
		
		String[] args = {"-s", "/tmp/settings", "-u"};
		Main main = new Main(mock);
		main.run(args);
		
		verify(mock).setHomeDir(Platform.getPlatform(main).getLocal());
	}
	
	public void test_userMode_local_config() throws Exception {
		JustAnotherPackageManager mock = mock(JustAnotherPackageManager.class);
		settings.put("jpm.cache.local", "/tmp/localCache");
		settings.save();
		
		String[] args = {"-s", "/tmp/settings","-u"};
		Main main = new Main(mock);
		main.run(args);

		verify(mock).setHomeDir(new File("/tmp/localCache"));
	}
	
	public void test_userMode_global_noConfig() throws Exception {
		JustAnotherPackageManager mock = mock(JustAnotherPackageManager.class);
		
		String[] args = {"-s", "/tmp/settings", "-g"};
		Main main = new Main(mock);
		main.run(args);
		
		verify(mock).setHomeDir(Platform.getPlatform(main).getGlobal());
	}
	
	public void test_userMode_global_config() throws Exception {
		JustAnotherPackageManager mock = mock(JustAnotherPackageManager.class);
		settings.put("jpm.cache.global", "/tmp/globalCache");
		settings.save();
		
		String[] args = {"-s", "/tmp/settings", "-g"};
		Main main = new Main(mock);
		main.run(args);

		verify(mock).setHomeDir(new File("/tmp/globalCache"));
	}
	
	public void test_userMode_runconfig() throws Exception {
		JustAnotherPackageManager mock = mock(JustAnotherPackageManager.class);
		settings.put("jpm.runconfig", "local");
		settings.save();
		
		String[] args = {"-s", "/tmp/settings"};
		Main main = new Main(mock);
		main.run(args);

		verify(mock).setHomeDir(Platform.getPlatform(main).getLocal());
	}
	
	public void test_userMode_default() throws Exception {
		JustAnotherPackageManager mock = mock(JustAnotherPackageManager.class);
		
		String[] args = {"-s", "/tmp/settings"};
		Main main = new Main(mock);
		main.run(args);

		verify(mock).setHomeDir(Platform.getPlatform(main).getGlobal());
	}
	
	public void test_install_local_binDir() throws Exception {
		
		
		
		settings.put("jpm.bin.local", "/tmp");
		settings.put("jpm.runconfig", "local");
		settings.save();
		
		String[] args = {"-s", "/tmp/settings", "install", "filemap"};
		Main main = new Main();
		// We have to make sure that service.jar is in the local repo (Junit cannot retrieve it from jar)
		// (considering that jpm is installed in its normal (global) place)
		IO.copy(new File(Platform.getPlatform(main).getGlobal(), "repo/service.jar"),
				new File(Platform.getPlatform(main).getLocal(), "repo/service.jar"));
		main.run(args);
		
		File exec = new File("/tmp/filemap");
		assertTrue(exec.exists());
		exec = null;
		
		String[] args2 = {"-s", "/tmp/settings", "remove", "filemap"};
		main.run(args2);
		exec = new File("/tmp/filemap");
		assertFalse(exec.exists());
	}
	
	/*public void testPierre() throws Exception {
		String[] args = {"deinit2", "-f"};
		Main main = new Main();
		main.run(args);
	}*/
}
