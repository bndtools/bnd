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
            p.store(new FileOutputStream(bndFile), "");

            Project bndProject = new Project(mockWS, null, bndFile);
            bndProject.setBase(baseDir);

            File imaginaryPom = new File(baseDir, "pom.xml");
            MavenProject mvnProject = new MavenProject();
            mvnProject.setFile(imaginaryPom);

            Util.setMavenDefaultsInBndProject(mvnProject, bndProject);
            Assert.assertEquals(mvnProject.getBasedir() + "/target/classes", bndProject.getOutput().getAbsolutePath());
            Assert.assertEquals(mvnProject.getBasedir() + "/target", bndProject.getTarget().getAbsolutePath());
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
