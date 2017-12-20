package biz.aQute.bnd.reporter.plugins;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import com.google.common.io.Files;
import java.io.File;
import junit.framework.TestCase;

public class WorkspaceSettingsEntryPluginTest extends TestCase {

  public void testWorkspaceSettingsEntry() throws Exception {

    final Processor p = new Processor();
    final WorkspaceSettingsEntryPlugin s = new WorkspaceSettingsEntryPlugin();

    assertTrue(s.extract(getWorkspace(), p) != null);
    assertTrue(p.isOk());
  }

  private Workspace getWorkspace() throws Exception {
    final File wsFile = Files.createTempDir();
    wsFile.deleteOnExit();

    final File bnd = new File(wsFile, "build.bnd");
    final File prop = new File(wsFile, "prop.properties");

    bnd.createNewFile();
    bnd.deleteOnExit();

    prop.createNewFile();
    prop.deleteOnExit();
    final Workspace ws = new Workspace(wsFile);

    return ws;
  }
}
