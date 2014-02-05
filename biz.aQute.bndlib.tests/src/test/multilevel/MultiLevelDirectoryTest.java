package test.multilevel;

import java.io.*;

import junit.framework.*;

public class MultiLevelDirectoryTest extends TestCase {

	public void testMultiLevelAntCall() {
		String home = System.getenv("ANT_HOME");
		if (home == null) {
			// Not implemented.
			return;
		}
		
		File binDir = new File(home, "bin");
		File bin = new File(binDir, "ant.bat");
		if (!bin.exists()) {
			bin = new File(binDir, "ant");
		}
		
		ProcessBuilder pb = new ProcessBuilder(bin.getAbsolutePath());
		File curDir = new File("..");
		File f = new File(curDir, "testing");
		File startDir = new File(f, "bndheirarchytest");
		pb.directory(startDir);
		Process p;
		try {
			p = pb.start();
			InputStream out = p.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(out));
			String str = null;
			while((str = in.readLine()) != null) {
				System.out.println(str);
			}
			int exitValue = p.waitFor();
			Assert.assertEquals(0, exitValue);
		}
		catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
