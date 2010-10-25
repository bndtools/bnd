package test;

import java.io.*;

import junit.framework.*;
import aQute.bnd.maven.*;
import aQute.lib.osgi.*;

public class MavenTest extends TestCase {

	public void testDeploy() throws FileNotFoundException, Exception {
		MavenCommand maven = new MavenCommand();
		maven.run(new String[]{"deploy","-temp", "temp", "-passphrase","jokulsarlon6128","tmp/biz.aQute.bnd.annotation.jar"}, 0);
		System.out.println(Processor.join(maven.getErrors(), "\n"));
		System.out.println(Processor.join(maven.getWarnings(), "\n"));
	}
	
	public void testSettings() throws FileNotFoundException, Exception {
		MavenCommand maven = new MavenCommand();
		maven.run(new String[]{"settings"}, 0);
		
		System.out.println(Processor.join(maven.getErrors(), "\n"));
		System.out.println(Processor.join(maven.getWarnings(), "\n"));
	}


}
