package biz.aQute.bnd.reporter.plugins;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import com.google.common.io.Files;
import java.io.File;
import junit.framework.TestCase;

public class ProjectSettingsEntryPluginTest extends TestCase {

  public void testProjectSettingsEntry() throws Exception {

    final Processor p = new Processor();
    final ProjectSettingsEntryPlugin s = new ProjectSettingsEntryPlugin();

    assertTrue(s.extract(getProject(), p) != null);
    assertTrue(p.isOk());
  }

  private Workspace getWorkspace() throws Exception {
    final File wsFile = Files.createTempDir();
    wsFile.deleteOnExit();

    final File bnd = new File(wsFile, "build.bnd");

    bnd.createNewFile();
    bnd.deleteOnExit();

    final Workspace ws = new Workspace(wsFile);

    return ws;
  }

  private Project getProject() throws Exception {
    final Workspace ws = getWorkspace();
    final File p = new File(ws.getBase(), "project1");
    final File bnd = new File(p, "bnd.bnd");

    p.mkdir();
    p.deleteOnExit();

    bnd.createNewFile();
    bnd.deleteOnExit();

    return ws.getProject("project1");
  }
}
