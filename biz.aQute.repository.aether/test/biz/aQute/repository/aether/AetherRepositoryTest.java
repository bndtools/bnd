package biz.aQute.repository.aether;

import java.io.File;
import java.io.FileInputStream;

import junit.framework.TestCase;
import aQute.bnd.deployer.repository.aether.AetherRepository;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;

public class AetherRepositoryTest extends TestCase {

	public void testDeploy() throws Exception {
		AetherRepository repo = new AetherRepository();
		File jar = new File("testdata/org.example.api.jar");
		
		PutOptions opts = new PutOptions();
		opts.type = PutOptions.BUNDLE;
		try {
			PutResult putResult = repo.put(new FileInputStream(jar), opts);
			System.out.println("Put completed");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
