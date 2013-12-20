package aQute.bnd.mavenplugin;

import java.io.*;
import java.util.*;

import org.apache.maven.execution.*;
import org.apache.maven.model.*;
import org.apache.maven.plugin.*;
import org.apache.maven.project.*;
import org.junit.*;
import org.mockito.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.lib.io.*;

public class ConfigureMavenProjectTest {
    private static int counter = 1;

    @Test
    public void testTransferLocationsToMaven() throws Exception {
    	File testProjDir = getTestDir();

    	try {
	    	Project mockBndProject = Mockito.mock(Project.class);
	    	Mockito.when(mockBndProject.isValid()).thenReturn(true);
	    	Mockito.when(mockBndProject.getBase()).thenReturn(testProjDir);
	    	Mockito.when(mockBndProject.getTarget()).thenReturn(new File(testProjDir, "bndtarget"));
	    	Mockito.when(mockBndProject.getSrc()).thenReturn(new File(testProjDir, "bnd/src"));
	    	Mockito.when(mockBndProject.getOutput()).thenReturn(new File(testProjDir, "bndoutput"));
	    	Mockito.when(mockBndProject.getTestSrc()).thenReturn(new File(testProjDir, "bnd/test"));
	    	Mockito.when(mockBndProject.getTestOutput()).thenReturn(new File(testProjDir, "testoutput"));
	    	Mockito.when(mockBndProject.getProperty(Constants.BUNDLE_VERSION, "0.0.0")).thenReturn("0.0.0");

	    	ConfigureMavenProject cmp = createCMP(mockBndProject, "0.0.0");

	    	cmp.execute();

	    	String baseDir = testProjDir.getAbsolutePath() + File.separator;
	    	Assert.assertEquals(baseDir + "bndtarget", cmp.project.getBuild().getDirectory());
	    	Assert.assertEquals(baseDir + "bnd/src", cmp.project.getBuild().getSourceDirectory());
	    	Assert.assertEquals(baseDir + "bndoutput", cmp.project.getBuild().getOutputDirectory());
	    	Assert.assertEquals(baseDir + "bnd/test", cmp.project.getBuild().getTestSourceDirectory());
	    	Assert.assertEquals(baseDir + "testoutput", cmp.project.getBuild().getTestOutputDirectory());
    	} finally {
            deleteDir(testProjDir);
    	}
    }

    @Test
    public void testCopyResources() throws Exception {
    	File testProjDir = getTestDir();

    	try {
	    	File srcRoot = new File(testProjDir, "src/main/java");
	    	createTestFile(new File(srcRoot, "org/foo/bar/Test.java"), "Testing 123");
	    	createTestFile(new File(srcRoot, "org/x/y/z/test.txt"), "testcontent");
	    	File testRoot = new File(testProjDir, "src/test/java");
	    	createTestFile(new File(testRoot, "someres.txt"), "XYZ");

    		Project mockBndProject = Mockito.mock(Project.class);
	    	Mockito.when(mockBndProject.isValid()).thenReturn(true);
	    	Mockito.when(mockBndProject.getBase()).thenReturn(testProjDir);
	    	Mockito.when(mockBndProject.getTarget()).thenReturn(new File(testProjDir, "target"));
			Mockito.when(mockBndProject.getSrc()).thenReturn(srcRoot);
	    	Mockito.when(mockBndProject.getOutput()).thenReturn(new File(testProjDir, "target/classes"));
	    	Mockito.when(mockBndProject.getTestSrc()).thenReturn(new File(testProjDir, "src/test/java"));
	    	Mockito.when(mockBndProject.getTestOutput()).thenReturn(new File(testProjDir, "target/test-classes"));
	    	Mockito.when(mockBndProject.getProperty(Constants.BUNDLE_VERSION, "0.0.0")).thenReturn("1.2.3.SNAPSHOT");

	    	ConfigureMavenProject cmp = createCMP(mockBndProject, "1.2.3-SNAPSHOT");

	    	cmp.execute();

	    	Assert.assertFalse("Should not copy the .java files",
	    			new File(testProjDir, "target/classes/org/foo/bar/Test.java").exists());
	    	assertFileContent("testcontent", new File(testProjDir, "target/classes/org/x/y/z/test.txt"));
	    	assertFileContent("XYZ", new File(testProjDir, "target/test-classes/someres.txt"));
    	} finally {
            deleteDir(testProjDir);
    	}
    }

	private void assertFileContent(String expectedContent, File file) throws IOException {
		Assert.assertEquals(expectedContent, IO.collect(file));
	}

	private void createTestFile(File file, String content) throws IOException {
		File dir = file.getParentFile();
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}

		FileOutputStream fos = new FileOutputStream(file);
		try {
			fos.write(content.getBytes());
		} finally {
			fos.close();
		}
	}

	private ConfigureMavenProject createCMP(Project mockBndProject, String version) throws Exception {
    	String artifactId = "my-artifact";

    	ConfigureMavenProject cmp = new ConfigureMavenProject();
		cmp.session = Mockito.mock(MavenSession.class);
		Workspace mockWorkSpace = Mockito.mock(Workspace.class);
		Mockito.when(mockWorkSpace.getProject(artifactId)).thenReturn(mockBndProject);
		cmp.bndWorkspace = Mockito.mock(BndWorkspace.class);
		Mockito.when(cmp.bndWorkspace.getWorkspace(cmp.session)).thenReturn(mockWorkSpace);

		Model model = new Model();
		model.setGroupId("my-group");
		model.setArtifactId(artifactId);
		model.setVersion(version);
		cmp.project = new MavenProject(model);
		return cmp;
	}

    @Test
    public void testSetBndDirsInMvnProject() throws Exception {
        Workspace mockWS = Mockito.mock(Workspace.class);
        Mockito.when(mockWS.getProperties()).thenReturn(new Properties());

        File baseDir = getTestDir();
        try {
            File bndFile = new File(baseDir, "bnd.bnd");
            Properties p = new Properties();
            p.put("src", "s1/s2/s3");
            p.put("bin", "b1");
            p.put("testsrc", "s1/s4");
            p.put("testbin", "b/t1");
            p.put("target-dir", "t");
            p.store(new FileOutputStream(bndFile), "");

            Project bndProject = new Project(mockWS, null, bndFile);
            bndProject.setBase(baseDir);
            bndProject.setProperty(Constants.BUNDLE_VERSION, "999");

            File imaginaryPom = new File(baseDir, "pom.xml");
            MavenProject mvnProject = new MavenProject();
            mvnProject.setFile(imaginaryPom);
            mvnProject.setVersion("999");

            ConfigureMavenProject.transferBndProjectSettingsToMaven(bndProject, mvnProject);
            Assert.assertEquals(mvnProject.getBasedir() + "/s1/s2/s3", mvnProject.getBuild().getSourceDirectory());
            Assert.assertEquals(mvnProject.getBasedir() + "/b1", mvnProject.getBuild().getOutputDirectory());
            Assert.assertEquals(mvnProject.getBasedir() + "/t", mvnProject.getBuild().getDirectory());
            Assert.assertEquals(mvnProject.getBasedir() + "/s1/s4", mvnProject.getBuild().getTestSourceDirectory());
            Assert.assertEquals(mvnProject.getBasedir() + "/b/t1", mvnProject.getBuild().getTestOutputDirectory());
        } finally {
            deleteDir(baseDir);
        }
    }

    @Test
    public void testBndProjectMvnProjectVersionMismatch() throws Exception {
        testBndProjectMvnProjectVersionMismatch("1.2.3.abc", "1.2.3.def", true);
        testBndProjectMvnProjectVersionMismatch("1.2.3.abc", "1.2.3-SNAPSHOT", true);
        testBndProjectMvnProjectVersionMismatch("1.2.3.SNAPSHOT", "1.2.3-SNAPSHOT", false);
        testBndProjectMvnProjectVersionMismatch("0.0.0", "0.0.0", false);
    }

	private void testBndProjectMvnProjectVersionMismatch(String bndVersion, String mvnVersion, boolean shouldFail) throws Exception {
		Workspace mockWS = Mockito.mock(Workspace.class);
        Mockito.when(mockWS.getProperties()).thenReturn(new Properties());

        File baseDir = getTestDir();

        try {
            File bndFile = new File(baseDir, "bnd.bnd");
            Properties p = new Properties();
            p.put(Constants.BUNDLE_VERSION, bndVersion);
            p.store(new FileOutputStream(bndFile), "");
	        Project bndProject = new Project(mockWS, null, bndFile);

	        MavenProject mvnProject = new MavenProject();
	        mvnProject.setVersion(mvnVersion);

	        try {
	            ConfigureMavenProject.transferBndProjectSettingsToMaven(bndProject, mvnProject);
	            if (shouldFail) {
	            	Assert.fail("Should fail on version mismatch");
	            }
	    	} catch (MojoExecutionException mjee) {
	    		if (!shouldFail) {
	    			throw new Exception("Should not report as failed", mjee);
	    		}
	    	}
        } finally {
            deleteDir(baseDir);
        }
	}

    private static synchronized File getTestDir() {
        File f = new File(System.getProperty("java.io.tmpdir") + "/" + ConfigureMavenProjectTest.class.getName() + "_" + (counter++));
        f.mkdirs();
        return f;
    }

    private static void deleteDir(File root) throws IOException {
        if (root.isDirectory()) {
            for (File file : root.listFiles()) {
                deleteDir(file);
            }
        }
        Assert.assertTrue(root.delete());
    }
}
