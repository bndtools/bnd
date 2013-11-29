package aQute.bnd.mavenplugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class UtilTest {
    private static int counter = 1;

    @Test
    public void testSetMavenDefaultsInBndProject() throws Exception {
        Workspace mockWS = Mockito.mock(Workspace.class);
        Mockito.when(mockWS.getProperties()).thenReturn(new Properties());

        File baseDir = getTestDir();
        try {
            File bndFile = new File(baseDir, "bnd.bnd");
            Properties p = new Properties();
            p.put("src", "s1/s2/s3");
            p.put("bin", "b1");
            p.put("target", "t");
            p.store(new FileOutputStream(bndFile), "");

            Project bndProject = new Project(mockWS, null, bndFile);
            bndProject.setBase(baseDir);

            File imaginaryPom = new File(baseDir, "pom.xml");
            MavenProject mvnProject = new MavenProject();
            mvnProject.setFile(imaginaryPom);

            Util.setBndDirsInMvnProject(mvnProject, bndProject);
            Assert.assertEquals(mvnProject.getBasedir() + "/s1/s2/s3", mvnProject.getBuild().getSourceDirectory());
            Assert.assertEquals(mvnProject.getBasedir() + "/b1", mvnProject.getBuild().getOutputDirectory());
            Assert.assertEquals(mvnProject.getBasedir() + "/t", mvnProject.getBuild().getDirectory());
        } finally {
            deleteDir(baseDir);
        }
    }

    private static synchronized File getTestDir() {
        File f = new File(System.getProperty("java.io.tmpdir") + "/" + UtilTest.class.getName() + "_" + (counter++));
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
