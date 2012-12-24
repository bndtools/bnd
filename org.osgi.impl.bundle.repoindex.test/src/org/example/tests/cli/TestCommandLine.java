package org.example.tests.cli;

import static org.example.tests.utils.Utils.copyToTempFile;
import static org.example.tests.utils.Utils.createTempDir;
import static org.example.tests.utils.Utils.deleteWithException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.example.tests.utils.Utils;

public class TestCommandLine extends TestCase {
	
	private static final String BINDEX2_LIB_PATH = "../org.osgi.impl.bundle.repoindex.cli/generated/org.osgi.impl.bundle.repoindex.cli.jar";
	
	private File tempDir;

	@Override
	protected void setUp() throws Exception {
		File index = new File("index.xml");
		if (index.exists())
			index.delete();
		
		File gzipIndex = new File("index.xml.gz");
		if (gzipIndex.exists())
			gzipIndex.delete();
		
		tempDir = createTempDir();
	}
	
	@Override
	protected void tearDown() throws Exception {
		deleteWithException(tempDir);
	}
	
	private void execute(String[] args) throws Exception {
		execute(args, false);
	}
	
	private void execute(String[] args, boolean runInTempDir) throws Exception {
		File jarFile = new File(BINDEX2_LIB_PATH);
		
		List<String> cmdLine = new LinkedList<String>();
		cmdLine.add("java");
//		cmdLine.add("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=9001,suspend=y");
		cmdLine.add("-jar");
		cmdLine.add(jarFile.getAbsolutePath());
		
		// set root URL
		if (!runInTempDir) {
			cmdLine.add("-d");
			cmdLine.add(tempDir.getAbsolutePath());
		}
		
		for (String arg : args) {
			cmdLine.add(arg);
		}
		
		File runDir = runInTempDir ? tempDir : new File("generated").getAbsoluteFile();
		System.out.println("Executing: " + cmdLine + " in directory " + runDir);
		ProcessBuilder builder = new ProcessBuilder(cmdLine)
			.directory(runDir)
			.redirectErrorStream(true);
		Process process = builder.start();
		
		String processOutput = Utils.readStream(process.getInputStream());
		System.err.println(processOutput);
		
		int returnCode = process.waitFor();
		if (returnCode != 0)
			throw new Exception("Process returned code " + returnCode);
	}

	public void testBasicCommandLine() throws Exception {
		File tempFile = copyToTempFile(tempDir, "testdata/01-bsn+version.jar");
		String[] args = new String[] {
				"--noincrement",
				tempFile.getAbsolutePath()
		};
		execute(args);
		assertTrue(new File("generated/index.xml.gz").exists());
		
		String formatted = Utils.readStream(getClass().getResourceAsStream("/testdata/expect.xml"));
		String expected = formatted.replaceAll("[\\n\\t]*", "");
		String actual = Utils.readStream(new GZIPInputStream(new FileInputStream("generated/index.xml.gz")));
		assertEquals(expected, actual);
	}
	
	public void testResourceInWorkingDir() throws Exception {
		File tempFile = new File(tempDir, "01-bsn+version.jar");
		Utils.copyFully(Utils.class.getResourceAsStream("/testdata/01-bsn+version.jar"), new FileOutputStream(tempFile));
		
		String[] args = new String[] {
				"--noincrement",
				"01-bsn+version.jar"
		};
		execute(args, true);
		File outputFile = new File(tempDir, "index.xml.gz");
		assertTrue(outputFile.exists());
		
		String formatted = Utils.readStream(getClass().getResourceAsStream("/testdata/expect-workingdir.xml"));
		String expected = formatted.replaceAll("[\\n\\t]*", "");
		String actual = Utils.readStream(new GZIPInputStream(new FileInputStream(outputFile)));
		assertEquals(expected, actual);
	}
	
	public void testUnknownArg() throws Exception {
		File tempFile = copyToTempFile(tempDir, "testdata/01-bsn+version.jar");
		String[] args = new String[] {
				"--nonsense",
				tempFile.getAbsolutePath()
		};
		try {
			execute(args);
			fail("Expected exception");
		} catch (Exception e) {
		}
	}
	
	public void testPrettyPrint() throws Exception {
		File tempFile = copyToTempFile(tempDir, "testdata/01-bsn+version.jar");
		String[] args = new String[] {
				"--pretty",
				"--noincrement",
				tempFile.getAbsolutePath()
		};
		execute(args);
		assertTrue(new File("generated/index.xml").exists());
		
 		String expected = Utils.readStream(getClass().getResourceAsStream("/testdata/expect-pretty.xml"));
		String actual = Utils.readStream(new FileInputStream("generated/index.xml"));
		assertEquals(expected, actual);
	}
	
	public void testKnownBundleRecognition() throws Exception {
		File tempFile = copyToTempFile(tempDir, "testdata/org.eclipse.equinox.ds-1.4.0.jar");
		String[] args = new String[] {
				"--pretty",
				"--noincrement",
				tempFile.getAbsolutePath()
		};
		execute(args);
		assertTrue(new File("generated/index.xml").exists());
		
 		String expected = Utils.readStream(getClass().getResourceAsStream("/testdata/org.eclipse.equinox.ds-1.4.0.xml"));
		String actual = Utils.readStream(new FileInputStream("generated/index.xml"));
		assertEquals(expected, actual);
	}
	
	public void testKnownBundleOverride() throws Exception {
		File tempFile = copyToTempFile(tempDir, "testdata/org.eclipse.equinox.ds-1.4.0.jar");
		String[] args = new String[] {
				"--pretty",
				"--noincrement",
				"-K",
				tempFile.getAbsolutePath()
		};
		execute(args);
		assertTrue(new File("generated/index.xml").exists());
		
 		String expected = Utils.readStream(getClass().getResourceAsStream("/testdata/org.eclipse.equinox.ds-1.4.0-overridden.xml"));
		String actual = Utils.readStream(new FileInputStream("generated/index.xml"));
		assertEquals(expected, actual);	}
	
	public void testKnownBundleExtra() throws Exception {
		Properties props = new Properties();
		props.setProperty("org.eclipse.equinox.ds;[1.4,1.5)", "cap=extra;extra=wibble");
		File knownBundlesFile = File.createTempFile("known", ".properties", new File("generated"));
		knownBundlesFile.deleteOnExit();
		props.store(new FileOutputStream(knownBundlesFile), "");
		
		File tempFile = copyToTempFile(tempDir, "testdata/org.eclipse.equinox.ds-1.4.0.jar");
		String[] args = new String[] {
				"--pretty",
				"--noincrement",
				"-k",
				knownBundlesFile.getName(),
				tempFile.getAbsolutePath()
		};
		execute(args);
		assertTrue(new File("generated/index.xml").exists());
		
 		String expected = Utils.readStream(getClass().getResourceAsStream("/testdata/org.eclipse.equinox.ds-1.4.0-extra.xml"));
		String actual = Utils.readStream(new FileInputStream("generated/index.xml"));
		assertEquals(expected, actual);
	}
}
