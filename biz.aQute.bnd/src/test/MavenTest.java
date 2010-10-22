package test;

import java.io.*;

import aQute.bnd.main.*;
import aQute.lib.osgi.*;
import junit.framework.*;

public class MavenTest extends TestCase {

	public void testDeploy() throws FileNotFoundException, Exception {
		Maven maven = new Maven();
		maven.run(new String[]{"deploy","-temp", "temp", "-passphrase","jokulsarlon6128","tmp/biz.aQute.bnd.annotation.jar"}, 0);
		System.out.println(Processor.join(maven.getErrors(), "\n"));
		System.out.println(Processor.join(maven.getWarnings(), "\n"));
	}
	
	public void testSettings() throws FileNotFoundException, Exception {
		Maven maven = new Maven();
		maven.run(new String[]{"settings"}, 0);
		
		System.out.println(Processor.join(maven.getErrors(), "\n"));
		System.out.println(Processor.join(maven.getWarnings(), "\n"));
	}


}
